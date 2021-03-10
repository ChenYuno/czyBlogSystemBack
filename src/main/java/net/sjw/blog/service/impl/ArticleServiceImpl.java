package net.sjw.blog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.SimTocExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.entity.Article;
import net.sjw.blog.entity.Labels;
import net.sjw.blog.entity.SearchPageList;
import net.sjw.blog.entity.User;
import net.sjw.blog.mapper.ArticleMapper;
import net.sjw.blog.mapper.LabelsMapper;
import net.sjw.blog.service.ArticleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.sjw.blog.service.ElasticsearchService;
import net.sjw.blog.service.LabelsService;
import net.sjw.blog.service.UserService;
import net.sjw.blog.utils.Constants;
import net.sjw.blog.utils.R;
import net.sjw.blog.utils.RedisUtils;
import net.sjw.blog.utils.TextUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author czy
 * @since 2020-10-13
 */
@Slf4j
@Transactional
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService, BaseService {

    @Autowired
    private TaskService taskService;
    @Autowired
    private UserService userService;


    @Autowired
    private ElasticsearchService elasticsearchService;

    /**
     * 后期可以去使用Quartz去做一些定时发布的功能
     * 如果是多人的博客系统得考虑审核的问题（工作流或状态的处理） -->审核通过通知、审核不通过也通知（包括理由之类的）
     * <p>
     * 保存成草稿的问题
     * 1、用户手动提交，会发送页面跳转
     * 2、代码自动提交、每隔一段时间就会提交 -> 不会发生页面跳转 ->多次提交 ->需要id唯一标识
     * <p>
     * 不管是哪种草稿 --> 必须有标题
     * <p>
     * 方案一：每次用户发布新文章之前 --> 先向后台请求一个唯一文章ID
     * 如果是更新文章，则不需要请求唯一id
     * <p>
     * 方案二：可以直接提交，后台判断id是否存在，没有就创建id，并且id作为返回结果
     * 如果有id就修改以及更新的内容
     * <p>
     * 推荐做法：
     * 自动保存草稿，在前端本地完成，也就是保存在本地。
     * 如果用户手动提交的，就提交到后台
     *
     * <p>
     * 防止重复提交(网络卡顿的时候，用户点了几次提交)
     * 可以通过id的方式
     * 通过tokenKey的提交频率来计算，如果30秒之内有多次提交，只有最前的一次有效
     * 其他的提交提示用户不要太频繁的提交
     * <p>
     * 前端也可以控制重复提交
     * 点击了提交按钮之后，禁止按钮使用，等到有响应的结果再改变按钮的状态
     * <p>
     *
     * @param article
     * @return
     */
    @Override
    public R postArticle(Article article) {

        //检查用户，获取用户信息
        User user = userService.checkUser();
        if (user == null) {
            return R.ACCOUNT_NOT_LOGIN();
        }
        //检查数据
        //title、分类ID、内容、类型、摘要、标签
        String title = article.getTitle();
        if (TextUtils.isEmpty(title)) {
            return R.FAILED("标题不可以为空");
        }
        //两种状态 草稿和发布
        String state = article.getState();
        if (!Constants.Article.STATE_PUBLIC.equals(state) &&
                !Constants.Article.STATE_DRAFT.equals(state)) {
            return R.FAILED("不支持此操作");
        }


        String type = article.getType();
        if (TextUtils.isEmpty(type)) {
            return R.FAILED("类型不可以为空");
        }
        if (!"0".equals(type) && !"1".equals(type)) {
            return R.FAILED("内容类型不对");
        }

        //下面的是发布的检查，草稿不需要检查
        if (Constants.Article.STATE_PUBLIC.equals(state)) {
            if (title.length() > Constants.Article.TITLE_MAX_LENGTH) {
                return R.FAILED("文章标题不可以超过" + Constants.Article.TITLE_MAX_LENGTH + "个字符");
            }
            String content = article.getContent();
            if (TextUtils.isEmpty(content)) {
                return R.FAILED("内容不可以为空");
            }

            String summary = article.getSummary();
            if (TextUtils.isEmpty(summary)) {
                return R.FAILED("摘要不可以为空");
            }
            if (summary.length() > Constants.Article.SUMMARY_MAX_LENGTH) {
                return R.FAILED("摘要不可以超出" + Constants.Article.SUMMARY_MAX_LENGTH + "个字符");
            }
            String labels = article.getLabels();
            //标签1-标签2-标签3
            if (TextUtils.isEmpty(labels)) {
                return R.FAILED("标签不可以为空");
            }
            String categoryId = article.getCategoryId();
            if (TextUtils.isEmpty(categoryId)) {
                return R.FAILED("文章分类不可以为空");
            }
        }


        String articleId = article.getId();
        if (TextUtils.isEmpty(articleId)) {

            article.setCreateTime(new Date());

        } else {
            //更新内容,对状态进行处理，如果已经发布了，则不能再保存为草稿
            Article articleFromDb = baseMapper.selectById(articleId);
            if (Constants.Article.STATE_PUBLIC.equals(articleFromDb.getState())
                    && Constants.Article.STATE_DRAFT.equals(state)) {
                //已经发布的只能更新，不能保存为草稿
                return R.FAILED("已经发布的不支持保存为草稿");
            }
            //到了这里说明没有保存为草稿，也是已经发布的状态，说明是更新
            log.info("没有保存为草稿，是是草稿的状态，说明是更新草稿");
            if (TextUtils.isEmpty(article.getCover())) {
                return R.FAILED("文章封面不能更新为空");
            }
            article.setUpdateTime(new Date());
            baseMapper.updateById(article);
            redisUtils.del(Constants.Article.KEY_ARTICLE_CACHE + articleId);
            redisUtils.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
            return R.SUCCESS("更新草稿成功").data("data", article.getId());
        }
        //补充数据
        //创建时间、用户id、更新时间

        if (TextUtils.isEmpty(article.getCover())) {

            article.setCover(Constants.Article.DEFAULT_COVER);
        }
        article.setUserId(user.getId());
        article.setUserName(user.getUserName());
        article.setUserAvatar(user.getAvatar());
        article.setUpdateTime(new Date());
        baseMapper.insert(article);

        //保存到数据库
        //保存到搜索的数据库里
        if (article.getState() == Constants.Article.STATE_PUBLIC) {

            elasticsearchService.addArticle(article);
        }

        //打散标签入库
        taskService.setupLabels(article.getLabels());
        //删除文章列表
        //TODO:rabbitMq ==> 监听:发一个消息就把相应的缓存删除，就不需要写太多的代码
        redisUtils.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);

        //返回结果,只有一种case会使用到这个id
        //如果要做程序自动保存为草稿(比如说30秒保存一次，就需要加上这个ID了，否则会创建多个item)
        return R.SUCCESS(
                Constants.Article.STATE_DRAFT.equals(state) ?
                        "草稿保存成功" : "文章发布成功")
                .data("data", article.getId());
    }

    /**
     * 管理中心获取文章
     *
     * @param page       页码
     * @param size       每一页数量
     * @param keyword    标题关键字
     * @param categoryId 分类id
     * @param state      状态：已经删除、草稿、已经发布、置顶
     * @return
     */
    @Override
    public R listArticles(int page, int size, String keyword, String categoryId, String state) {
        //处理page、size
        page = checkPage(page);
        size = size <= 5 ? 5 : size;

        boolean isSearch = !TextUtils.isEmpty(categoryId) || !TextUtils.isEmpty(keyword) || !TextUtils.isEmpty(state);

        //第一页座缓存
        if (page == 1 && !isSearch) {
            String articleListJson = (String) redisUtils.get(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
            if (!TextUtils.isEmpty(articleListJson)) {
                SearchPageList<Article> result = gson.fromJson(articleListJson, new TypeToken<SearchPageList<Article>>() {
                }.getType());
                log.info("article from redis ...");
                return R.SUCCESS("获取文章列表成功").data("data", result);
            }
        }
        //创建分页和排序条件
        //开始查询
        //处理查询条件
        //增加id：id来查询文章的功能,使用多一点if
        Page<Article> all;
        if (!TextUtils.isEmpty(keyword)) {
            if (keyword.startsWith("id:")) {
                keyword = keyword.substring(3);
                all = baseMapper.selectPage(new Page<>(page, size), Wrappers.<Article>lambdaQuery()
                        .select(
                                Article::getId,
                                Article::getTitle,
                                Article::getUserId,
                                Article::getUserName,
                                Article::getUserAvatar,
                                Article::getContent,
                                Article::getCover,
                                Article::getState,
                                Article::getType,
                                Article::getCategoryId,
                                Article::getLabels,
                                Article::getSummary,
                                Article::getViewCount,
                                Article::getCreateTime,
                                Article::getUpdateTime
                        )
                        .eq(!TextUtils.isEmpty(state), Article::getState, state)
                        .ne(Article::getType, "0")
                        .eq(!TextUtils.isEmpty(categoryId), Article::getCategoryId, categoryId)
                        .eq(Article::getId, keyword)
                        .orderByDesc(Article::getCreateTime));
            } else {
                all = baseMapper.selectPage(new Page<>(page, size), Wrappers.<Article>lambdaQuery()
                        .select(
                                Article::getId,
                                Article::getTitle,
                                Article::getUserId,
                                Article::getUserName,
                                Article::getUserAvatar,
                                Article::getContent,
                                Article::getCover,
                                Article::getState,
                                Article::getType,
                                Article::getCategoryId,
                                Article::getLabels,
                                Article::getSummary,
                                Article::getViewCount,
                                Article::getCreateTime,
                                Article::getUpdateTime
                        )
                        .eq(!TextUtils.isEmpty(state), Article::getState, state)
                        .ne(Article::getType, "0")
                        .eq(!TextUtils.isEmpty(categoryId), Article::getCategoryId, categoryId)
                        .like(Article::getTitle, keyword)
                        .orderByDesc(Article::getCreateTime));
            }
        } else {
            all = baseMapper.selectPage(new Page<>(page, size), Wrappers.<Article>lambdaQuery()
                    .select(
                            Article::getId,
                            Article::getTitle,
                            Article::getUserId,
                            Article::getUserName,
                            Article::getUserAvatar,
                            Article::getContent,
                            Article::getCover,
                            Article::getState,
                            Article::getType,
                            Article::getCategoryId,
                            Article::getLabels,
                            Article::getSummary,
                            Article::getViewCount,
                            Article::getCreateTime,
                            Article::getUpdateTime
                    )
                    .eq(!TextUtils.isEmpty(state), Article::getState, state)
                    .ne(Article::getType, "0")
                    .eq(!TextUtils.isEmpty(categoryId), Article::getCategoryId, categoryId)
                    .orderByDesc(Article::getCreateTime));
        }


        SearchPageList<Article> dataToRedis = new SearchPageList<>(all);
        if (page == 1 && !isSearch) {

            redisUtils.set(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE,
                    gson.toJson(dataToRedis), Constants.TimeSecond.MIN_15);
        }
        log.info("article from mysql ...");

        //返回结果
        return R.SUCCESS("获取文章列表成功").data("data", dataToRedis);
    }


    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private Gson gson;

    /**
     * 如果有审核机制：审核中的文章只有管理员和作者自己可以获取
     * 有草稿、删除、置顶、已经发布
     * （草稿和删除只有管理员可以获取）
     *
     * <p></p>
     * 统计文章的阅读量
     * 要精确一点的话要对IP进行处理，如果是同一IP，则不保存
     * TODO:可以使用hutool的jar来对ip进行解析，还可以之后集成地图对ip解析可以知道用户的所在城市
     * <p>
     * 先把阅读量在redis里缓存一份，比如10分钟
     * 当文章没的时候，从mysql中取，这个时间更新阅读量
     * 10分钟后，下一次访问的时候更新一次阅读量
     *
     * @param articleId
     * @return
     */
    @Override
    public R getArticleById(String articleId) {
        //先从redis中获取
        //如果没有再去mysql中获取
        String articleJson = (String) redisUtils.get(Constants.Article.KEY_ARTICLE_CACHE + articleId);
        if (!TextUtils.isEmpty(articleJson)) {
            Article article = gson.fromJson(articleJson, Article.class);
            redisUtils.incr(Constants.Article.KEY_ARTICLE_VIEW_COUNT + articleId, 1);
            //返回结果
            return R.SUCCESS("获取文章成功").data("data", article);
        }

        //查询出文章
        Article article = baseMapper.getArticleWithUser(articleId);
        if (article == null) {
            return R.FAILED("文章不存在");
        }
        article.setLabel();
        //markdown to html
//        MutableDataSet options = new MutableDataSet().set(Parser.EXTENSIONS, Arrays.asList(
//                TablesExtension.create(),
//                JekyllTagExtension.create(),
//                TocExtension.create(),
//                SimTocExtension.create()
//        ));
//        Parser parser = Parser.builder(options).build();
//
//        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
//        Node parse = parser.parse(article.getContent());
//        String html = renderer.render(parse);
//        Article copyArticleHaveHtml = new Article();
//        BeanUtils.copyProperties(article, copyArticleHaveHtml);
//        copyArticleHaveHtml.setContent(html);
        //判断文章状态
        String state = article.getState();
        if (Constants.Article.STATE_PUBLIC.equals(state) ||
                Constants.Article.STATE_TOP.equals(state)) {
            //正常发布的状态，才可以增加阅读量
            redisUtils.set(Constants.Article.KEY_ARTICLE_CACHE + articleId,
                    gson.toJson(article), Constants.TimeSecond.MIN_5);
            //设置阅读量的key，先从redis里面拿，如果redis里没有，就从article中取，并且追加到redis里
            Integer viewCount = (Integer) redisUtils.get(Constants.Article.KEY_ARTICLE_VIEW_COUNT + articleId);
            if (null == viewCount) {
                Integer viewCountFromDb = article.getViewCount();
                redisUtils.set(Constants.Article.KEY_ARTICLE_VIEW_COUNT + articleId, viewCountFromDb + 1);
            } else {
                //有的话更新到mysql中
                Integer newCount = (int) redisUtils.incr(Constants.Article.KEY_ARTICLE_VIEW_COUNT + articleId, 1);
                article.setViewCount(newCount);
                baseMapper.updateById(article);
                //更新ES里面的阅读量
                elasticsearchService.updateArticle(articleId, article);
            }


            return R.SUCCESS("获取文章成功").data("data", article);
        }
        //如果是删除/草稿，需要角色管理员
        User user = userService.checkUser();
        if (user == null || !Constants.User.ROLE_ADMIN.equals(user.getRoles())) {
            return R.PERMISSION_DENIED();
        }
        //返回结果
        return R.SUCCESS("获取文章成功").data("data", article);
    }


    /**
     * 更新文章内容
     * 想着更新是不是独立出一个接口更好。。。
     * <p>
     * 该接口只支持修改内容：内容、标题、分类、标签、摘要
     *
     * @param articleId
     * @param article
     * @return
     */
    @Override
    public R updateArticle(String articleId, Article article) {
        Article articleFromDb = baseMapper.selectById(articleId);
        if (articleFromDb == null) {
            return R.FAILED("文章不存在");
        }
        if (TextUtils.isEmpty(article.getCover())) {
            return R.FAILED("请选择文章封面");
        }
        //内容修改
        String title = article.getTitle();
        if (!TextUtils.isEmpty(title)) {
            articleFromDb.setTitle(title);
        }
        String summary = article.getSummary();
        if (!TextUtils.isEmpty(summary)) {
            articleFromDb.setSummary(summary);
        }
        String content = article.getContent();
        if (!TextUtils.isEmpty(content)) {
            articleFromDb.setContent(content);
        }
        String labels = article.getLabels();
        if (!TextUtils.isEmpty(labels)) {
            articleFromDb.setLabels(labels);
        }
        String category = article.getCategoryId();
        if (!TextUtils.isEmpty(category)) {
            articleFromDb.setCategoryId(category);
        }
        String cover = article.getCover();
        if (!cover.equals(articleFromDb.getCover())) {
            articleFromDb.setCover(cover);
        }
        String userName = article.getUserName();
        String userAvatar = article.getUserAvatar();
        articleFromDb.setUserName(userName);
        articleFromDb.setUserAvatar(userAvatar);
        if (!article.getState().equals("3")) {

            articleFromDb.setState("1");
        } else {
            articleFromDb.setState(article.getState());
        }

        articleFromDb.setUpdateTime(new Date());

        baseMapper.updateById(articleFromDb);
        redisUtils.del(Constants.Article.KEY_ARTICLE_CACHE + articleId);
        redisUtils.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
        elasticsearchService.updateArticle(articleId,articleFromDb);
        return R.SUCCESS("更新文章成功");
    }

    /**
     * 删除文章 --> 物理删除
     *
     * @param articleId
     * @return
     */
    @Override
    public R deleteArticle(String articleId) {
        //这里不用删除评论
        //因为在数据库的设计那里调整为里级联删除
        int result = baseMapper.deleteById(articleId);
        if (result > 0) {

            redisUtils.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
            redisUtils.del(Constants.Article.KEY_ARTICLE_CACHE + articleId);
            //删除Es那里的内容
            elasticsearchService.deleteArticle(articleId);

            return R.SUCCESS("文章删除成功");
        }
        return R.FAILED("文章不存在");
    }

    /**
     * 通过修改状态删除文章
     *
     * @param articleId
     * @return
     */
    @Override
    public R deleteArticleByUpdateState(String articleId) {
        int result = baseMapper.update(null, Wrappers.<Article>lambdaUpdate()
                .set(Article::getState, "0").eq(Article::getId, articleId));
        if (result > 0) {
            redisUtils.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
            redisUtils.del(Constants.Article.KEY_ARTICLE_CACHE + articleId);
            //删除Es那里的内容
            elasticsearchService.deleteArticle(articleId);
            return R.SUCCESS("文章删除成功");
        }
        return R.FAILED("文章不存在");
    }

    @Override
    public R topArticle(String articleId) {
        Article article = baseMapper.selectById(articleId);
        if (article == null) {
            return R.FAILED("文章不存在");
        }
        String state = article.getState();
        //必须是已经发布的文章才可以置顶
        if (Constants.Article.STATE_PUBLIC.equals(state)) {
            article.setUpdateTime(new Date());
            article.setState(Constants.Article.STATE_TOP);
            baseMapper.updateById(article);
            redisUtils.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
            return R.SUCCESS("文章置顶成功");
        }
        //如果已经置顶的文章就取消置顶
        if (Constants.Article.STATE_TOP.equals(state)) {
            article.setUpdateTime(new Date());
            article.setState(Constants.Article.STATE_PUBLIC);
            baseMapper.updateById(article);
            redisUtils.del(Constants.Article.KEY_ARTICLE_LIST_FIRST_PAGE);
            return R.SUCCESS("文章取消置顶");
        }
        if (Constants.Article.STATE_DELETE.equals(state)) {
            return R.FAILED("删除的文章不能置顶");
        } else {
            return R.FAILED("草稿的文章不能置顶");
        }
    }

    /**
     * 获取置顶文章
     * 和权限无关
     * 状态必须置顶
     *
     * @return
     */
    @Override
    public R listTopArticles() {
        List<Article> all = baseMapper.selectList(Wrappers.<Article>lambdaQuery()
                .eq(Article::getState, Constants.Article.STATE_TOP)
                .ne(Article::getType, "0")
                .orderByDesc(Article::getUpdateTime));
        return R.SUCCESS("获取置顶文章列表成功").data("data", all);
    }


    @Autowired
    private Random random;

    /**
     * 获取推荐文章，通过标签来计算
     *
     * @param articleId
     * @param size
     * @return
     */
    @Override
    public R listRecommendArticle(String articleId, int size) {
        List<String> labels = new ArrayList<>();
        baseMapper.selectObjs(Wrappers.<Article>lambdaQuery()
                .select(Article::getLabels)
                .eq(Article::getId, articleId)).stream()
                .map(m -> (String) m)
                .forEach(label -> {
                    if (label.contains("-")) {
                        String[] split = label.split("-");
                        for (String s : split) {
                            labels.add(s);
                        }
                    } else {
                        labels.add(label);
                    }
                });
        //从列表中随机获取一标签，查询此标签相似的文章
        String targetLabel = labels.get(random.nextInt(labels.size()));

        List<Article> likeResultList = baseMapper.selectList(Wrappers.<Article>lambdaQuery()
                .like(Article::getLabels, targetLabel)
                .and(state -> state.eq(Article::getState, Constants.Article.STATE_PUBLIC)
                        .or()
                        .eq(Article::getState, Constants.Article.STATE_TOP))
                .ne(Article::getId, articleId)
                .ne(Article::getType, "0")
                .last(" limit " + size));
        //判断长度
        if (likeResultList.size() < size) {
            int suppement = size - likeResultList.size();
            List<Article> suppleArticles = baseMapper.selectList(Wrappers.<Article>lambdaQuery()
                    .and(state -> state.eq(Article::getState, Constants.Article.STATE_PUBLIC)
                            .or()
                            .eq(Article::getState, Constants.Article.STATE_TOP))
                    .ne(Article::getId, articleId)
                    .ne(Article::getType, "0")
                    .orderByDesc(Article::getCreateTime)
                    .last(" limit " + suppement));
            //过滤掉补充重复的文章
            List<Article> filterSuppleArticle = suppleArticles.stream().filter(article -> {
                String articleSuppleId = article.getId();
                for (Article a : likeResultList) {
                    if (articleSuppleId.equals(a.getId())) {
                        return false;
                    }
                }
                return true;
            }).collect(Collectors.toList());
            likeResultList.addAll(filterSuppleArticle);

        }
        for (Article res:likeResultList) {
            res.setLabel();
        }
        return R.SUCCESS("获取文章成功").data("data", likeResultList);
    }

    @Override
    public R listArticleByLabel(int page, int size, String label) {
        page = checkPage(page);
        size = checkSize(size);

        Page<Article> all = baseMapper.selectPage(new Page<>(page, size), Wrappers.<Article>lambdaQuery()
                .like(Article::getLabels, label)
                .and(a -> a.eq(Article::getState, Constants.Article.STATE_PUBLIC).or()
                        .eq(Article::getState, Constants.Article.STATE_TOP))
                .orderByDesc(Article::getCreateTime));


        return R.SUCCESS("获取文章列表成功").data("data", all);
    }


    @Autowired
    private LabelsMapper labelsMapper;

    @Override
    public R listLabels(int size) {
        size = checkSize(size);
        List<Labels> all = labelsMapper.selectList(Wrappers.<Labels>lambdaQuery()
                .orderByDesc(Labels::getCount).last(" limit " + size));
        return R.SUCCESS("获取标签列表成功").data("data", all);
    }

    @Override
    public R getArticleCount() {
        int count = this.baseMapper.selectCount(Wrappers.<Article>lambdaQuery().ne(Article::getType, "0"));
        return R.SUCCESS("文章总数获取成功.").data("data", count);
    }

    @Override
    public R totalArticlePublishCount() {
        Integer publishArticlesCount = baseMapper.selectCount(Wrappers.<Article>lambdaQuery()
                .eq(Article::getState, Constants.Article.STATE_PUBLIC)
                .ne(Article::getType, "0"));
        return R.SUCCESS("获取发表文章总数成功.").data("data",publishArticlesCount);
    }

    @Override
    public R getArchiveArticles() {
        List<Article> dule = baseMapper.selectList(Wrappers.<Article>lambdaQuery()
                .select(
                        Article::getId,
                        Article::getTitle,
                        Article::getUserId,
                        Article::getUserName,
                        Article::getUserAvatar,
                        Article::getCover,
                        Article::getState,
                        Article::getType,
                        Article::getCategoryId,
                        Article::getLabels,
                        Article::getSummary,
                        Article::getViewCount,
                        Article::getCreateTime,
                        Article::getUpdateTime
                )
                .ne(Article::getType, "0")
                .orderByDesc(Article::getCreateTime));
        for (int i = 0; i < dule.size(); i++) {
            dule.get(i).setLabel();
        }
        SimpleDateFormat sfY = new SimpleDateFormat("yyyy");
        SimpleDateFormat sfM = new SimpleDateFormat("MM");
        Map<String, Map<String,List<Article>>> data = new HashMap<>();//年的集合-》月的集合-》月份里面有的全部文章
        for (Article article : dule) {
            String year = sfY.format(article.getCreateTime());
            String month = sfM.format(article.getCreateTime());
            if (!data.containsKey(year)) {
                Map<String, List<Article>> map = new HashMap<>();
                List<Article> articleList = new ArrayList<>();
                articleList.add(article);
                map.put(month, articleList);
                data.put(year, map);
            } else {
                Map<String, List<Article>> map = data.get(year);
                if (!map.containsKey(month)) {
                    List<Article> articleList = new ArrayList<>();
                    articleList.add(article);
                    map.put(month, articleList);
                } else {
                    List<Article> articleList = map.get(month);
                    articleList.add(article);
                }
            }
        }
        return R.SUCCESS("归档文章数据获取成功!").data("data",data);
    }
}
