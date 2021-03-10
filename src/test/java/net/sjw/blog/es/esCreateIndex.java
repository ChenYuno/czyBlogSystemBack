package net.sjw.blog.es;

import com.google.gson.Gson;
import net.sjw.blog.entity.Article;
import net.sjw.blog.mapper.ArticleMapper;
import net.sjw.blog.utils.EsSaveArticleUtils;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.ml.PostDataRequest;
import org.elasticsearch.cluster.metadata.MetaDataIndexTemplateService;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@SpringBootTest
public class esCreateIndex {

    @Autowired
    @Qualifier("searchclient")
    private RestHighLevelClient client;

    @Autowired
    private ArticleMapper articleMapper;

    @Test
    void createPropertiesMapping() throws IOException {

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


    }

    @Autowired
    @Qualifier("esDate")
    private SimpleDateFormat format;

    @Autowired
    private Gson gson;

    @Test
    void addAllArticles() throws IOException {
        List<Article> articles = articleMapper.selectList(null);



        for (int i = 0; i < articles.size(); i++) {
            IndexRequest request = new IndexRequest("czy_blog_article");


            Article article = articles.get(i);

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
            String s = EsSaveArticleUtils.markdownToText(article.getContent());
            data.put("content", s);
            data.put("type", article.getType());
            data.put("userId", article.getUserId());
            data.put("userName", article.getUserName());
            data.put("userAvatar", article.getUserAvatar());
            data.put("viewCount", article.getViewCount());
            data.put("createTime", format.format(article.getCreateTime()));
            data.put("updateTime", format.format(article.getUpdateTime()));

            request.source(gson.toJson(data), XContentType.JSON);

            IndexResponse response = client.index(request, RequestOptions.DEFAULT);

            System.out.println(response.getIndex());
            System.out.println(response.status());


        }


    }

    //1317493105812852737
    @Test
    void addAArticle()throws IOException {
        Article article = articleMapper.selectById("1318850309384171521");
        IndexRequest request = new IndexRequest("czy_blog_article");




        request.id(article.getId());
        request.timeout(TimeValue.timeValueSeconds(1));
        request.timeout("10s");

        Map<String, Object> data = new HashMap<>();
        data.put("id", article.getId());
        data.put("state", article.getState());
        data.put("labels", article.getLabels());
        data.put("cover", article.getCover());
        data.put("title", article.getTitle());
        data.put("categoryId", article.getCategoryId());
        data.put("summary", article.getSummary());
        String s = EsSaveArticleUtils.markdownToText(article.getContent());
        data.put("content", s);
        data.put("type", article.getType());
        data.put("userId", article.getUserId());
        data.put("userName", article.getUserName());
        data.put("userAvatar", article.getUserAvatar());
        data.put("viewCount", article.getViewCount());
        data.put("createTime", format.format(article.getCreateTime()));
        data.put("updateTime", format.format(article.getUpdateTime()));

        request.source(gson.toJson(data), XContentType.JSON);

        IndexResponse response = client.index(request, RequestOptions.DEFAULT);

        System.out.println(response.getIndex());
        System.out.println(response.status());
    }

    @Test
    void testInit() throws IOException {

        GetIndexRequest request = new GetIndexRequest("aa");
        request.humanReadable(true);
        boolean exists = client.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println(exists);
    }

    @Test
    void test01() {
        Article article = articleMapper.selectById("1318850309384171521");
        Article copyBean = new Article();
        BeanUtils.copyProperties(article, copyBean);
        System.out.println(copyBean);
    }


    @Test
    void test02() {
        System.out.println(reorganizeString("zhymo"));
    }

    public String reorganizeString(String S) {
        if(S.length()<2)return S;
        char[] chars = S.toCharArray();
        Map<Character,Integer> map = new HashMap<>();
        for(char c:chars){
            if(!map.containsKey(c)){
                map.put(c,1);
                continue;
            }
            map.put(c,map.get(c)+1);
        }
        int len = S.length();
        List<Character> list  =new LinkedList<>();

        int judgeMax = Integer.MIN_VALUE;

        for(char key:map.keySet()){
            list.add(key);
            int val = map.get(key);
            if(val>judgeMax)judgeMax = val;
        }
        if(judgeMax>(len+1)/2)return "";
        list.sort((a,b)->{
            return map.get(b) - map.get(a);
        });

        //System.out.println(list);

        StringBuilder sb = new StringBuilder(S.length());
        int index1 = 0;
        int index2 = 1;
        while(list.size()>1){

            char key1 = list.get(index1);
            char key2 = list.get(index2);
            int val1 = map.get(key1);
            int val2 = map.get(key2);
            sb.append(key1);
            map.put(key1,val1-1);
            map.put(key2,val2-1);
            int removeIndex = 0;
            boolean flag = false;
            if(map.get(key1) == 0){
                list.remove(removeIndex);
                list.sort((a,b)->{
                    return map.get(b) - map.get(a);
                });
                continue;
                // flag = true;
            }
            sb.append(key2);
            removeIndex = flag?0:1;
            if(map.get(key2) == 0){
                list.remove(removeIndex);
            }
        }
        sb.append(list.remove(0));
        // System.out.println(list+"    <lll");
        // System.out.println(list.size()+" - "+map.get(list.get(0))+"   "+list.get(0));
        // if(list.size() == 1 && map.get(list.get(0)) ==1){
        //     System.out.println(list+"    <lll");


        // }
        return sb.toString();
    }
}
