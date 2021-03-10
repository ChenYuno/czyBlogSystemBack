package net.sjw.blog.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.entity.Article;
import net.sjw.blog.entity.Comment;
import net.sjw.blog.entity.SearchPageList;
import net.sjw.blog.entity.User;
import net.sjw.blog.mapper.ArticleMapper;
import net.sjw.blog.mapper.CommentMapper;
import net.sjw.blog.service.CommentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.sjw.blog.service.UserService;
import net.sjw.blog.utils.Constants;
import net.sjw.blog.utils.R;
import net.sjw.blog.utils.RedisUtils;
import net.sjw.blog.utils.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

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
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService, BaseService {


    @Autowired
    private UserService userService;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private TaskService taskService;


    @Autowired
    private RedisUtils redisUtils;

    /**
     * 发表评论
     *
     * @param comment 评论
     * @return
     */
    @Override
    public R postComment(Comment comment) {
        //检查用户是否登录
        User user = userService.checkUser();
        if (user == null) {
            return R.ACCOUNT_NOT_LOGIN();
        }
        //检查内容
        String articleId = comment.getArticleId();
        if (TextUtils.isEmpty(articleId)) {
            return R.FAILED("文章ID不可以为空");
        }
        Article article = articleMapper.selectOne(Wrappers.<Article>lambdaQuery()
                .eq(Article::getId, articleId));
        if (article == null) {
            return R.FAILED("文章不存在");
        }
        if (article.getState().equals(Constants.Article.STATE_DRAFT) ||
                article.getState().equals(Constants.Article.STATE_DELETE)) {
            return R.FAILED("评论的文章只能是已经发布的。。😑");
        }
        String content = comment.getContent();
        if (TextUtils.isEmpty(content)) {
            return R.FAILED("评论内容不可以为空");
        }
        //补全内容
        comment.setUpdateTime(new Date());
        comment.setCreateTime(new Date());
        comment.setUserAvatar(user.getAvatar());
        comment.setUserName(user.getUserName());
        comment.setUserId(user.getId());
        //保存入库
        baseMapper.insert(comment);
        //发送邮件通知
        taskService.notifyCriticzedAuthor(articleId, user);
        //清除对应文章的评论缓存
        redisUtils.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE_CACHE + articleId);


        //新提交的评论先让它在上面五分钟
        Page<Comment> all = baseMapper.selectPage(new Page<>(1, 10), Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getArticleId, articleId)
                .isNull(Comment::getParentContentId)
                .and(c -> c.eq(Comment::getState, "1").or().eq(Comment::getState, "2"))
                .orderByDesc(Comment::getState)
                .orderByDesc(Comment::getCreateTime));
        List<Comment> allRecords = all.getRecords();
        for (int i = 0; i < allRecords.size(); i++) {
            recursionToFindAllChildrenCommends(allRecords.get(i));
        }
        //把结果转为pageList
        SearchPageList<Comment> dataToCache = new SearchPageList<Comment>(all);


        redisUtils.set(Constants.Comment.KEY_COMMENT_FIRST_PAGE_CACHE + articleId,
                gson.toJson(dataToCache),Constants.TimeSecond.MIN_5);

        //返回结果
        return R.SUCCESS("评论成功");
    }


    @Autowired
    private Gson gson;
    /**
     * 获取文章的评论
     * 评论的排序策略
     * 最基本的就是按时间排序-->升序和降序-->先发表的在前面或者后后发表的在前面
     * <p>
     * 置顶的一定在最前面
     * <p>
     * 后发表的：前单位时间内会拍在前面，过了此单位时间，会按点赞量和发表时间进行排序
     *
     * @param articleId
     * @param page
     * @param size
     * @param isMore
     * @return
     */
    @Override
    public R listCommentByArticleId(String articleId, int page, int size, String isMore) {
        page = checkPage(page);
        size = checkSize(size);
        //如果是第一页，那我们先从缓存中获取
        if (page == 1 && isMore == null) {
            String cacheJson = (String) redisUtils.get(Constants.Comment.KEY_COMMENT_FIRST_PAGE_CACHE + articleId);
            if (!TextUtils.isEmpty(cacheJson)) {
                log.info("comment from redis ...");
                SearchPageList<Comment> result = gson.fromJson(cacheJson, new TypeToken<SearchPageList<Comment>>() {
                }.getType());
                return R.SUCCESS("评论列表获取成功").data("data", result);
            }
        }
        //如果有就返回
        //没有就往下走
        Page<Comment> all = baseMapper.selectPage(new Page<>(page, size), Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getArticleId, articleId)
                .isNull(Comment::getParentContentId)
                .and(c -> c.eq(Comment::getState, "1").or().eq(Comment::getState, "2"))
                .orderByDesc(Comment::getState)
                .orderByDesc(Comment::getCreateTime));
        List<Comment> allRecords = all.getRecords();
        for (int i = 0; i < allRecords.size(); i++) {
            recursionToFindAllChildrenCommends(allRecords.get(i));
        }
        //把结果转为pageList
        SearchPageList<Comment> dataToCache = new SearchPageList<Comment>(all);
        //保存一份到缓存里
        if (page == 1&&isMore == null) {
            redisUtils.set(Constants.Comment.KEY_COMMENT_FIRST_PAGE_CACHE + articleId,
                    gson.toJson(dataToCache),Constants.TimeSecond.MIN_5);

        }
        log.info("comment from mysql ==> "+dataToCache);
        return R.SUCCESS("评论列表获取成功").data("data", dataToCache);
    }

    //递归查出所有子评论
    public void recursionToFindAllChildrenCommends(Comment currCommend) {
        List<Comment> childrenCommends = baseMapper.selectList(Wrappers.<Comment>lambdaQuery()
                .eq(Comment::getParentContentId, currCommend.getId()));
        if (!childrenCommends.isEmpty()) {
            for (int i = 0; i < childrenCommends.size(); i++) {
                recursionToFindAllChildrenCommends(childrenCommends.get(i));
            }
            currCommend.getChildrenCommends().addAll(childrenCommends);
        }
    }

    @Override
    public R deleteCommentById(String commentId) {
        //检查用户角色
        User user = userService.checkUser();
        if (user == null) {
            return R.ACCOUNT_NOT_LOGIN();
        }
        //把评论找出来，对比用户权限
        Comment comment = baseMapper.selectById(commentId);
        if (comment == null) {
            return R.FAILED("评论不存在");
        }
        if (user.getId().equals(comment.getUserId())||
                Constants.User.ROLE_ADMIN.equals(user.getRoles())) {
            baseMapper.deleteById(commentId);
            String articleId = comment.getArticleId();
            redisUtils.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE_CACHE + articleId);
            return R.SUCCESS("评论删除成功");
        }
        return R.PERMISSION_DENIED();

    }

    @Override
    public R deleteComment(String commentId) {
        Comment comment = baseMapper.selectById(commentId);
        if (comment == null) {
            return R.FAILED("评论不存在");
        }
        comment.setUpdateTime(new Date());

        if (!comment.getState().equals("0")) {
            comment.setState("0");
            baseMapper.updateById(comment);
            redisUtils.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE_CACHE + comment.getArticleId());
            return R.SUCCESS("该评论变得不可用了");
        }
        comment.setState("1");
        redisUtils.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE_CACHE + comment.getArticleId());
        baseMapper.updateById(comment);
        return R.SUCCESS("恢复了该评论的使用");
    }

    @Override
    public R listComment(int page, int size, String commentId) {
        page = checkPage(page);
        size = checkSize(size);
        Page<Comment> preDate = baseMapper.selectPage(new Page<>(page, size), Wrappers.<Comment>lambdaQuery()
                .like(!TextUtils.isEmpty(commentId), Comment::getId, commentId)
                .orderByDesc(Comment::getUpdateTime));
        SearchPageList<Comment> all = new SearchPageList<>(preDate);
        return R.SUCCESS().data("data", all);
    }

    @Override
    public R topComment(String commentId) {
        Comment comment = baseMapper.selectById(commentId);
        if (comment == null) {
            return R.FAILED("置顶评论不存在");
        }
        String state = comment.getState();
        if (state.equals("0")) {
            return R.FAILED("被删除的评论不能置顶");
        }
        String message;
        if (state.equals("1")) {
            comment.setState("2");
            message = "评论置顶成功";
        } else {
            comment.setState("1");
            message = "评论取消置顶";
        }
        comment.setUpdateTime(new Date());

        baseMapper.updateById(comment);
        redisUtils.del(Constants.Comment.KEY_COMMENT_FIRST_PAGE_CACHE + comment.getArticleId());
        return R.SUCCESS(message);
    }

    @Override
    public R updateCommentUserInfo() {
        List<Comment> comments = baseMapper.selectList(null);
        for (int i = 0; i < comments.size(); i++) {
            Comment comment = comments.get(i);
            User user = userService.getBaseMapper().selectOne(Wrappers.<User>lambdaQuery()
                    .select(User::getUserName, User::getAvatar)
                    .eq(User::getId,comment.getUserId()));

            comment.setUserName(user.getUserName());
            comment.setUserAvatar(user.getAvatar());
            baseMapper.updateById(comment);
            System.out.println("updateUserInfoFromComment Finish!!!!!!!!!!!!");
        }
        return R.SUCCESS();
    }

    @Override
    public R getCommentCount() {
        int count = this.count();
        return R.SUCCESS("获取评论总数成功.").data("data", count);
    }


}
