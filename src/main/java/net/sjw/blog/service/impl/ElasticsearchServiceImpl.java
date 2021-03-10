package net.sjw.blog.service.impl;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.sjw.blog.entity.Article;
import net.sjw.blog.entity.SearchPageList;
import net.sjw.blog.mapper.ArticleMapper;
import net.sjw.blog.service.ElasticsearchService;
import net.sjw.blog.utils.Constants;
import net.sjw.blog.utils.EsSaveArticleUtils;
import net.sjw.blog.utils.R;
import net.sjw.blog.utils.TextUtils;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * 时机：
 * 搜索内容添加：
 * 文章发表的时候
 * <p></p>
 * 搜索内容删除的时候
 * 文章删除的时候
 * <p></p>
 * 搜索内容更新的时候
 * //当阅读量更新的时候
 */
@Slf4j
@Service
public class ElasticsearchServiceImpl implements ElasticsearchService,BaseService {

    @Autowired
    @Qualifier("esDate")
    private  SimpleDateFormat format ;

    @Autowired
    private Gson gson;
    @Autowired
    @Qualifier("searchclient")
    private RestHighLevelClient client;

    @Autowired
    private ArticleMapper articleMapper;


    @Async
    @Override
    public void addArticle(Article article) {
        try {
            log.info("===========================\n" +
                    "开始检测索引存不存在");
            GetIndexRequest indexRequest = new GetIndexRequest("czy_blog_article");
            boolean exists = client.indices().exists(indexRequest, RequestOptions.DEFAULT);
            System.out.println(exists);
            if (!exists) {
                log.info("索引不存在，准备初始化索引");
                initIndexCzyBlogArticle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        log.info("看到这里代表的是已经初始化好文档或是文档已经存在，准备吧第一篇文章添加或更新文章进去");
        try {
            IndexRequest request = new IndexRequest("czy_blog_article");
            request.id(article.getId());
            request.timeout(TimeValue.timeValueSeconds(1));
            request.timeout("1s");

            Map<String, Object> data = new HashMap<>();
            data.put("id", article.getId());
            data.put("state", article.getState());
            data.put("labels", article.getLabels());
            data.put("cover", article.getCover());
            data.put("title", article.getTitle());
            data.put("categoryId", article.getCategoryId());
            data.put("summary", article.getSummary());
            String markdownToTextContent = EsSaveArticleUtils.markdownToText(article.getContent());
//            log.info(markdownToTextContent+" ||||||look!!!");
            data.put("content",markdownToTextContent);
            data.put("type", article.getType());
            data.put("userId", article.getUserId());
            data.put("userName", article.getUserName());
            data.put("userAvatar", article.getUserAvatar());
            data.put("viewCount", article.getViewCount());
            data.put("createTime", format.format(article.getCreateTime()));
            data.put("updateTime", format.format(article.getUpdateTime()));

            request.source(gson.toJson(data), XContentType.JSON);

            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            log.info("添加文章成功！！！"+
                    response.getIndex()+"  ==  "+response.status());
            System.out.println();
        } catch (Exception e) {
            log.info("添加文章异常..."+e.getMessage()+"\n" +
                    e.getCause());
        }

    }

    private void initIndexCzyBlogArticle() {
        try {
            CreateIndexRequest request = new CreateIndexRequest("czy_blog_article");
            XContentBuilder builder = XContentFactory.jsonBuilder();
            builder.startObject();
            {
                builder.startObject("properties");
                {
                    builder.startObject("id");
                    {
                        builder.field("type", "keyword");
                    }
                    builder.endObject();
                    builder.startObject("title");
                    {
                        builder.field("type", "text");
                        builder.field("index", "true");
                        builder.field("analyzer", "ik_max_word");
                    }
                    builder.endObject();
                    builder.startObject("userId");
                    {
                        builder.field("type", "keyword");
                    }
                    builder.endObject();
                    builder.startObject("userAvatar");
                    {
                        builder.field("type", "keyword");
                    }
                    builder.endObject();
                    builder.startObject("userName");
                    {
                        builder.field("type", "text");
                        builder.field("analyzer", "ik_max_word");
                    }
                    builder.endObject();
                    builder.startObject("cover");
                    {
                        builder.field("type", "keyword");
                    }
                    builder.endObject();
                    builder.startObject("categoryId");
                    {
                        builder.field("type", "keyword");
                    }
                    builder.endObject();
                    builder.startObject("content");
                    {
                        builder.field("type", "text");
                        builder.field("analyzer", "ik_max_word");
                    }
                    builder.endObject();
                    builder.startObject("type");
                    {
                        builder.field("type", "keyword");
                    }
                    builder.endObject();
                    builder.startObject("state");
                    {
                        builder.field("type", "keyword");
                    }
                    builder.endObject();
                    builder.startObject("summary");
                    {
                        builder.field("type", "text");
                        builder.field("analyzer", "ik_max_word");
                    }
                    builder.endObject();
                    builder.startObject("labels");
                    {
                        builder.field("type", "text");
                        builder.field("analyzer", "ik_max_word");
                    }
                    builder.endObject();
                    builder.startObject("viewCount");
                    {
                        builder.field("type", "long");
                    }
                    builder.endObject();
                    builder.startObject("createTime");
                    {
                        builder.field("type", "date");
                        builder.field("format", "yyyy-MM-dd HH:mm:ss");
                    }
                    builder.endObject();
                    builder.startObject("updateTime");
                    {
                        builder.field("type", "date");
                        builder.field("format", "yyyy-MM-dd HH:mm:ss");
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endObject();



            request.mapping(builder);

            CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
            System.out.println("response.isAcknowledged  ==>  " + response.isAcknowledged());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public R doSearch(String keyword, int page, int size, String categoryId, Integer sort) {
        //1、检查page和size
        page = checkPage(page);
//        size = checkSize(size);
        size = size <= 5 ? 5 : size;

        //2、分页设置
        //第一页from = 0
        //第二页from = size
        //第三页from = 2*size ...
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        int from = (page - 1) * size;
        searchSourceBuilder.size(size).from(from);
        // 3、设置搜索结果
        // 关键字
        //过滤条件
        log.info(keyword);
        if (TextUtils.isEmpty(keyword)) {
//            searchSourceBuilder.query(new MatchAllQueryBuilder());
//            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
//            List<QueryBuilder> should = boolQueryBuilder.must();
//            should.add(QueryBuilders.matchQuery("title", ""));

            System.out.println("====================");
            System.out.println(keyword);
            System.out.println(categoryId);
            System.out.println(sort);
            System.out.println("====================");
//            if (!TextUtils.isEmpty(categoryId)) {
//                should.add(QueryBuilders.termQuery("categoryId", categoryId));
                searchSourceBuilder.query(QueryBuilders.termQuery("categoryId", categoryId));
//            }



        } else {

            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            List<QueryBuilder> should = boolQueryBuilder.should();
            should.add(QueryBuilders.matchQuery("content", keyword));
            should.add(QueryBuilders.matchQuery("summary", keyword));
            should.add(QueryBuilders.matchQuery("userName", keyword));
            should.add(QueryBuilders.matchQuery("labels", keyword));

            List<QueryBuilder> must = boolQueryBuilder.must();
            must.add(QueryBuilders.matchQuery("title", keyword));

            if (!TextUtils.isEmpty(categoryId)) {
                must.add(QueryBuilders.termQuery("categoryId", categoryId));
            }

            searchSourceBuilder.query(boolQueryBuilder);

        }

        //排序
        //排序有四个 1)根据时间的升序 2）降序 3）根据浏览量的升序 4）降序
        if (sort != null) {
            if (sort == 1) {
                searchSourceBuilder.sort("createTime", SortOrder.ASC);
            } else if (sort == 2) {
                searchSourceBuilder.sort("createTime", SortOrder.DESC);
            }else if (sort == 3) {
                searchSourceBuilder.sort("viewCount",SortOrder.ASC);
            }else if (sort == 4) {
                searchSourceBuilder.sort("viewCount",SortOrder.DESC);
            }
        }
        //关键字高亮

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.field("content");
        highlightBuilder.field("labels");
        highlightBuilder.field("summary");
        highlightBuilder.field("userName");
        highlightBuilder.requireFieldMatch(true);//多个高亮显示
        highlightBuilder.preTags("<span style='color:red;'>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);


        //4、处理搜索结果
        searchRequest.indices("czy_blog_article");


        try {
            SearchResponse response = client.search(searchRequest.source(searchSourceBuilder), RequestOptions.DEFAULT);

            SearchHits hits = response.getHits();
            SearchHit[] hitsHits = hits.getHits();

            //用来封装结果
            SearchPageList<Article> resultToSearch = new SearchPageList<>(page,size,hitsHits.length);


            for (SearchHit hitsHit : hitsHits) {
                //未处理过高亮返回的数据
                Map<String, Object> sourceAsMap = hitsHit.getSourceAsMap();

                //高亮的数据，需要和原来的数据进行结合
                Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
                log.info("highlightFields  numbers："+highlightFields.size());
                HighlightField title = highlightFields.get("title");
                packageHighField(sourceAsMap, title, "title");
//                HighlightField content = highlightFields.get("content");
//                packageHighField(sourceAsMap, content, "content");
                HighlightField labels = highlightFields.get("labels");
                packageHighField(sourceAsMap, labels, "labels");
                HighlightField summary = highlightFields.get("summary");
                packageHighField(sourceAsMap,summary,"summary");
                HighlightField userName = highlightFields.get("userName");
                packageHighField(sourceAsMap, userName, "userName");

                resultToSearch.getRecords().add(new Article()
                        .setUserName((String) sourceAsMap.get("userName"))
                        .setTitle((String) sourceAsMap.get("title"))
                        .setContent((String) sourceAsMap.get("content"))
                        .setLabels((String) sourceAsMap.get("labels"))
                        .setId((String) sourceAsMap.get("id"))
                        .setUserId((String) sourceAsMap.get("userId"))
                        .setUserAvatar((String) sourceAsMap.get("userAvatar"))
                        .setCover((String) sourceAsMap.get("cover"))
                        .setViewCount((Integer) sourceAsMap.get("viewCount"))
                        .setUpdateTime(format.parse((String) sourceAsMap.get("updateTime")))
                        .setCreateTime(format.parse((String) sourceAsMap.get("createTime")))
                        .setCategoryId((String) sourceAsMap.get("categoryId"))
                        .setSummary((String) sourceAsMap.get("summary")));


            }


            return R.SUCCESS("搜索成功").data("data", resultToSearch);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 5、返回搜索结果
        return R.FAILED("搜索失败");
    }

    @Override
    public void deleteArticle(String articleId) {
        try {
            DeleteRequest deleteRequest = new DeleteRequest(Constants.Article.ES_ARTICLE_INDEX, articleId);
            DeleteResponse delete = client.delete(deleteRequest, RequestOptions.DEFAULT);
            System.out.println(delete.status());  //查看删除状态
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Async
    @Override
    public void updateArticle(String articleId, Article article) {
        article.setId(articleId);
        this.addArticle(article);
    }

    private void packageHighField(Map<String, Object> sourceAsMap, HighlightField highLightFromMap, String esField) {
        if (highLightFromMap != null) {
            StringBuilder sb = new StringBuilder();
            Text[] fragments = highLightFromMap.fragments();
            for (Text fragment : fragments) {
                sb.append(fragment);
            }
            log.info(esField+"  ==>"+sb.toString());
            sourceAsMap.put(esField, sb.toString());
        }
    }
}
