package net.sjw.blog.testEs;

import com.google.gson.Gson;
import com.vladsch.flexmark.ext.jekyll.tag.JekyllTagExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.toc.SimTocExtension;
import com.vladsch.flexmark.ext.toc.TocExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import net.sjw.blog.entity.Article;
import net.sjw.blog.mapper.ArticleMapper;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;


import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@SpringBootTest
public class Test01 {

    @Autowired
    @Qualifier("searchclient")
    private RestHighLevelClient client;
    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private Gson gson;

    /**
     * 测试文章id：1317491045897883650
     */
    @Test
    public void test01() throws IOException {
        Article article = articleMapper.selectById("1317493105812852737");
        //创建请求
        IndexRequest request = new IndexRequest("yun_index");
        //规则 put /yun_index/_doc/1
        request.id(article.getId());
        request.timeout(TimeValue.timeValueSeconds(1));
        request.timeout("1s");
        //将我们的数据放入请求json
        //把对象转化为json
        request.source(gson.toJson(article), XContentType.JSON);
        //客户端发送请求,获取响应的结构
        IndexResponse index = client.index(request, RequestOptions.DEFAULT);
        System.out.println(index.toString());
        //对应我们命令返回的状态CREATED
        System.out.println(index.status());
    }

    //获取文档，先要判断是否存在
    @Test
    void testIsExists() throws IOException {
        //1317491045897883650
        GetRequest getRequest = new GetRequest("yun_index", "1317491045897883650");
        //不获取返回的_source的上下文，这么做效率更高
        getRequest.fetchSourceContext(new FetchSourceContext(false));
        getRequest.storedFields("_none_");
        boolean exists = client.exists(getRequest, RequestOptions.DEFAULT);
        System.out.println(exists);
    }


    //获取文档信息
    @Test
    void testGetDocument() throws IOException {
        GetRequest getRequest = new GetRequest("yun_index", "1317491045897883650");
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        String sourceAsString = getResponse.getSourceAsString();


        String content = (String) getResponse.getSource().get("content");

        System.out.println(content);


        //markdown to html
        MutableDataSet options = new MutableDataSet().set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                JekyllTagExtension.create(),
                TocExtension.create(),
                SimTocExtension.create()
        ));
        Parser parser = Parser.builder(options).build();

        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        Node parse = parser.parse(content);
        String html = renderer.render(parse);
        //html to text
        String text = Jsoup.parse(html).text();

        System.out.println("html:  " + html);
        System.out.println("text:  " + text);


        System.out.println(sourceAsString);//打印文档到的内容
    }

    //修改文档信息
    @Test
    void updateDocument() throws IOException {
        UpdateRequest updateRequest = new UpdateRequest("yun_index", "1317491045897883650");
        updateRequest.timeout("1s");
        Article article = articleMapper.selectById("1317493105812852737");
        article.setContent("|||<span style='color:red'>yeyeyeye</span>|||");
        updateRequest.doc(gson.toJson(article), XContentType.JSON);
        UpdateResponse update = client.update(updateRequest, RequestOptions.DEFAULT);
        System.out.println(update.status());  //查看更新状态
    }

    //删除文档信息
    @Test
    void deleteDocument() throws IOException {
        DeleteRequest deleteRequest = new DeleteRequest("yun_index", "1317491045897883650");
        DeleteResponse delete = client.delete(deleteRequest, RequestOptions.DEFAULT);
        System.out.println(delete.status());  //查看删除状态
    }

    //测试批量添加文档信息
    @Test
    void bulkDocument() throws IOException {
        //创建批量操作对象
        BulkRequest bulkRequest = new BulkRequest();
        bulkRequest.timeout("10s");

        List<Article> articles = articleMapper.selectList(null);

        for (int i = 0; i < articles.size(); i++) {
            bulkRequest.add(
                    new IndexRequest("yun_index").id(articles.get(i).getId())
                            .source(gson.toJson(articles.get(i)), XContentType.JSON));
        }
        //发送请求
        BulkResponse bulk = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        System.out.println(bulk.hasFailures());  //查看状态，是否失败，返回false代表成功
    }

    //测试查询文档信息
    //SearchRequest 搜索请求
    //SearchSourceBuilder 条件构造
    //HighlightBuilder 构造高亮
    //TermQueryBuilder 精确查询
    //MatchQueryBuilder 匹配所有
    @Test
    void search() throws IOException {
        //创建请求对象
        SearchRequest searchRequest = new SearchRequest("yun_index");
        //构造搜索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //使用工具类构造搜索信息
        //查询条件，我们可以使用QueryBuilders工具来实现
        //QueryBuilders.termQuery精确
        //QueryBuilders.matchQuery()匹配所有
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("labels", "java").analyzer("ik_max_word");
        searchSourceBuilder.query(matchQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        //发送请求
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(gson.toJson(searchResponse.getHits()));  //Hits对象就包含查询的各种信息
        System.out.println("--------------------------------------------------");
        for (SearchHit documentFields : searchResponse.getHits().getHits()) {
            System.out.println(documentFields.getSourceAsMap());
        }
    }

    @Test
    void textuseanalysis() throws IOException {

        CreateIndexRequest request = new CreateIndexRequest("use_analysis");

        request.mapping(
                "{\n" +
                        "  \"properties\": {\n" +
                        "    \"title\": {\n" +
                        "      \"type\": \"text\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}",
                XContentType.JSON);
        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
        System.out.println(response.isAcknowledged());
        System.out.println(response.isShardsAcknowledged());


        GetMappingsRequest getMappingsRequest = new GetMappingsRequest();
        GetMappingsRequest indices = getMappingsRequest.indices("use_analysis");
        GetMappingsResponse mapping = client.indices().getMapping(indices, RequestOptions.DEFAULT);
        Map<String, MappingMetaData> mappings =
                mapping.mappings();
        for (Map.Entry<String, MappingMetaData> stringMappingMetaDataEntry : mappings.entrySet()) {
            System.out.println("key: " + stringMappingMetaDataEntry.getKey() + "  | val : " + stringMappingMetaDataEntry.getValue().type());
        }


    }

    @Test
    void testBool() throws IOException {
        SearchRequest request = new SearchRequest();
        request.indices("yun_index");

        SearchSourceBuilder builder = new SearchSourceBuilder();

        BoolQueryBuilder bool = new BoolQueryBuilder();
        List<QueryBuilder> must = bool.must();
        must.add(QueryBuilders.matchQuery("title", "数据"));
        must.add(QueryBuilders.matchQuery("content", "测试"));

        builder.query(bool);
        request.source(builder);

        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();

            for (Map.Entry<String, Object> stringObjectEntry : sourceAsMap.entrySet()) {
                System.out.println(stringObjectEntry.getKey() + " == " + stringObjectEntry.getValue());
            }

        }
    }

    @Test
    void testMapping() throws IOException {

        CreateIndexRequest request = new CreateIndexRequest("test_mapping");
        XContentBuilder mapping = XContentFactory.jsonBuilder().startObject();
        mapping.startObject("properties");
        {

            mapping.startObject("name");
            {
                mapping.field("type", "text");
            }
            mapping.endObject();
            mapping.startObject("sn");
            {
                mapping.field("type", "keyword");
            }
            mapping.endObject();
        }
        mapping.endObject();
        mapping.endObject();

        request.mapping(mapping);

        CreateIndexResponse response = client.indices().create(request, RequestOptions.DEFAULT);
    }

    @Test
    void withIk() throws IOException {
        SearchRequest request = new SearchRequest();
        request.indices("test_mapping");

        SearchSourceBuilder builder = new SearchSourceBuilder();
        builder.query(QueryBuilders.matchQuery("name", "云").analyzer("ik_max_word"));

        request.source(builder);

        SearchResponse search = client.search(request, RequestOptions.DEFAULT);

        SearchHit[] hits = search.getHits().getHits();

        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            System.out.println(sourceAsString);
        }
    }

    @Test
    void t1() {


    }
}
