package com.nowcoder.community;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.event.EventConsumer;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.SearchHit;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.util.*;

/**
 * ClassName: ElasticsearchTests
 * Package: com.nowcoder.community
 * Description:
 *
 * @Autuor Dongjie Sang
 * @Create 2023/6/7 20:31
 * @Version 1.0
 */
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {

    private static final Logger logger = LoggerFactory.getLogger(ElasticsearchTests.class);

    @Autowired
    private DiscussPostMapper discussMapper;

    @Autowired
    private DiscussPostRepository discussRepository;

    //@Autowired
    //private ElasticsearchTemplate elasticTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Test
    void testInsert() {
        discussRepository.save(discussMapper.selectDiscussPostById(241));
        discussRepository.save(discussMapper.selectDiscussPostById(242));
        discussRepository.save(discussMapper.selectDiscussPostById(243));
    }

    @Test
    void testInsertList() {
        discussRepository.saveAll(discussMapper.selectDiscussPosts(101, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(102, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(103, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(111, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(112, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(131, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(132, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(133, 0, 100,0));
        discussRepository.saveAll(discussMapper.selectDiscussPosts(134, 0, 100,0));
    }

    @Test
    void testUpdate() {
        DiscussPost post = discussMapper.selectDiscussPostById(231);
        post.setContent("我是新人，使劲灌水。");
        discussRepository.save(post);
    }

    @Test
    void testDelete() {
        discussRepository.deleteById(231);
        // 删除所有数据
        discussRepository.deleteAll();
    }

    //不带高亮的查询
    @Test
    public void noHighlightQuery() throws IOException {
        SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名

        //构建搜索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                //在discusspost索引的title和content字段中都查询“互联网寒冬”
                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                // matchQuery是模糊查询，会对key进行分词：searchSourceBuilder.query(QueryBuilders.matchQuery(key,value));
                // termQuery是精准查询：searchSourceBuilder.query(QueryBuilders.termQuery(key,value));
                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                //一个可选项，用于控制允许搜索的时间：searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
                .from(0)// 指定从哪条开始查询
                .size(10);// 需要查出的总记录条数

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

//        System.out.println(JSONObject.toJSON(searchResponse));

        List<DiscussPost> list = new LinkedList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);
//            System.out.println(discussPost);
            list.add(discussPost);
        }
        System.out.println(list.size());
        for (DiscussPost post : list) {
            System.out.println(post);
        }


        logger.info("===== 查询结果 =====");
        logger.info("结果数量: {}", list.size());
        logger.info("结果明细:");
        list.forEach(post -> logger.info(post.toString()));

        // 同时保留控制台输出
        System.out.println("控制台输出 - 结果数量: " + list.size());
        list.forEach(System.out::println);

    }




    // 别人写的 大概看了下文档，没问题直接用了
    @Test
    public void highlightQuery() throws Exception{
        SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名
        Map<String,Object> res = new HashMap<>();

        //高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.field("content");
        highlightBuilder.requireFieldMatch(false);
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");

        //构建搜索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .from(0)// 指定从哪条开始查询
                .size(10)// 需要查出的总记录条数
                .highlighter(highlightBuilder);//高亮
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        //System.out.println(JSONObject.toJSON(searchResponse));

        List<DiscussPost> list = new ArrayList<>();
        long total = searchResponse.getHits().getTotalHits().value;
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);

            // 处理高亮显示的结果
            HighlightField titleField = hit.getHighlightFields().get("title");
            if (titleField != null) {
                discussPost.setTitle(titleField.getFragments()[0].toString());
            }
            HighlightField contentField = hit.getHighlightFields().get("content");
            if (contentField != null) {
                discussPost.setContent(contentField.getFragments()[0].toString());
            }
//            System.out.println(discussPost);
            list.add(discussPost);
        }
        res.put("list",list);
        res.put("total",total);
        if(res.get("list")!= null){
            for (DiscussPost post : list = (List<DiscussPost>) res.get("list")) {
                System.out.println(post);
            }
            System.out.println(res.get("total"));
        }
    }
}
//package com.nowcoder.community;
//
//import com.alibaba.fastjson.JSONObject;
//import com.nowcoder.community.dao.DiscussPostMapper;
//import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
//import com.nowcoder.community.entity.DiscussPost;
//import org.elasticsearch.action.search.SearchRequest;
//import org.elasticsearch.action.search.SearchResponse;
//import org.elasticsearch.client.RequestOptions;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.index.query.QueryBuilders;
//import org.elasticsearch.search.builder.SearchSourceBuilder;
//import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
//import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
//import org.elasticsearch.search.sort.SortBuilders;
//import org.elasticsearch.search.sort.SortOrder;
//import org.elasticsearch.search.SearchHit;
//import org.junit.jupiter.api.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.io.IOException;
//import java.util.*;
//
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//@ContextConfiguration(classes = CommunityApplication.class)
//public class ElasticsearchTests {
//
//    @Autowired
//    private DiscussPostMapper discussMapper;
//
//    @Autowired
//    private DiscussPostRepository discussRepository;
//
//    @Autowired
//    private RestHighLevelClient restHighLevelClient;
//
////    @Autowired
////    private ElasticsearchTemplate elasticTemplate;
//
////    @Autowired
////    private ElasticsearchOperations elasticsearchOperations;
//
//
//    @Test
//    public void testInsert() {
//        discussRepository.save(discussMapper.selectDiscussPostById(241));
//        discussRepository.save(discussMapper.selectDiscussPostById(242));
//        discussRepository.save(discussMapper.selectDiscussPostById(243));
//    }
////
//    @Test
//    public void testInsertList() {
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(101, 0, 100));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(102, 0, 100));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(103, 0, 100));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(111, 0, 100));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(112, 0, 100));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(131, 0, 100));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(132, 0, 100));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(133, 0, 100));
//        discussRepository.saveAll(discussMapper.selectDiscussPosts(134, 0, 100));
//    }
//
//    @Test
//    public void testUpdate() {
//        DiscussPost post = discussMapper.selectDiscussPostById(231);
//        post.setContent("我是新人,使劲灌水.");
//        discussRepository.save(post);
//    }
//
//    @Test
//    public void testDelete() {
//         discussRepository.deleteById(231);
////        discussRepository.deleteAll();
//    }
//
//
//    //不带高亮的查询
//    @Test
//    public void noHighlightQuery() throws IOException {
//        SearchRequest searchRequest = new SearchRequest("discusspost");//discusspost是索引名，就是表名
//
//        //构建搜索条件
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
//                //在discusspost索引的title和content字段中都查询“互联网寒冬”
//                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
//                // matchQuery是模糊查询，会对key进行分词：searchSourceBuilder.query(QueryBuilders.matchQuery(key,value));
//                // termQuery是精准查询：searchSourceBuilder.query(QueryBuilders.termQuery(key,value));
//                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
//                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
//                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
//                //一个可选项，用于控制允许搜索的时间：searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
//                .from(0)// 指定从哪条开始查询
//                .size(10);// 需要查出的总记录条数
//
//        searchRequest.source(searchSourceBuilder);
//        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
//
//        //System.out.println(JSONObject.toJSON(searchResponse));
//
//        List<DiscussPost> list = new LinkedList<>();
//        for (SearchHit hit : searchResponse.getHits().getHits()) {
//            DiscussPost discussPost = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);
//            //System.out.println(discussPost);
//            list.add(discussPost);
//        }
//        System.out.println(list.size());
//        for (DiscussPost post : list) {
//            System.out.println(post);
//        }
//    }
//
//
////    public List<DiscussPost> searchDiscussPost(String keyword, int page, int size) throws IOException {
////        SearchResponse<DiscussPost> response = elasticsearchClient.search(s -> s
////                        .index("discusspost") // 索引名（小写）
////                        .query(q -> q
////                                .multiMatch(m -> m
////                                        .fields("title", "content")
////                                        .query(keyword)
////                                )
////                        )
////                        .highlight(h -> h
////                                .fields("title", f -> f.preTags("<em>").postTags("</em>"))
////                                .fields("content", f -> f.preTags("<em>").postTags("</em>"))
////                        )
////                        .sort(s1 -> s1.field(f -> f.field("type").order(SortOrder.Desc)))
////                        .sort(s2 -> s2.field(f -> f.field("score").order(SortOrder.Desc)))
////                        .sort(s3 -> s3.field(f -> f.field("createTime").order(SortOrder.Desc)))
////                        .from(page * size)
////                        .size(size),
////                DiscussPost.class
////        );
////
////        List<DiscussPost> result = new ArrayList<>();
////        for (Hit<DiscussPost> hit : response.hits().hits()) {
////            DiscussPost post = hit.source();
////            if (post != null && hit.highlight() != null) {
////                Map<String, List<String>> highlight = hit.highlight();
////                if (highlight.containsKey("title")) {
////                    post.setTitle(highlight.get("title").get(0));
////                }
////                if (highlight.containsKey("content")) {
////                    post.setContent(highlight.get("content").get(0));
////                }
////            }
////            result.add(post);
////        }
////
////        return result;
////    }
//
//
////    @Test
////    public void testSearchByRepository() {
////        SearchQuery searchQuery = new NativeSearchQueryBuilder()
////                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
////                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
////                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
////                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
////                .withPageable(PageRequest.of(0, 10))
////                .withHighlightFields(
////                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
////                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
////                ).build();
//
//        // elasticTemplate.queryForPage(searchQuery, class, SearchResultMapper)
//        // 底层获取得到了高亮显示的值, 但是没有返回.
////
////        Page<DiscussPost> page = discussRepository.search(searchQuery);
////        System.out.println(page.getTotalElements());
////        System.out.println(page.getTotalPages());
////        System.out.println(page.getNumber());
////        System.out.println(page.getSize());
////        for (DiscussPost post : page) {
////            System.out.println(post);
////        }
////    }
//
//
////    @Test
////    public void testSearchByTemplate() {
////        SearchQuery searchQuery = new NativeSearchQueryBuilder()
////                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
////                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
////                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
////                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
////                .withPageable(PageRequest.of(0, 10))
////                .withHighlightFields(
////                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
////                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
////                ).build();
////
////        Page<DiscussPost> page = elasticTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
////            @Override
////            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
////                SearchHits hits = response.getHits();
////                if (hits.getTotalHits() <= 0) {
////                    return null;
////                }
////
////                List<DiscussPost> list = new ArrayList<>();
////                for (SearchHit hit : hits) {
////                    DiscussPost post = new DiscussPost();
////
////                    String id = hit.getSourceAsMap().get("id").toString();
////                    post.setId(Integer.valueOf(id));
////
////                    String userId = hit.getSourceAsMap().get("userId").toString();
////                    post.setUserId(Integer.valueOf(userId));
////
////                    String title = hit.getSourceAsMap().get("title").toString();
////                    post.setTitle(title);
////
////                    String content = hit.getSourceAsMap().get("content").toString();
////                    post.setContent(content);
////
////                    String status = hit.getSourceAsMap().get("status").toString();
////                    post.setStatus(Integer.valueOf(status));
////
////                    String createTime = hit.getSourceAsMap().get("createTime").toString();
////                    post.setCreateTime(new Date(Long.valueOf(createTime)));
////
////                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
////                    post.setCommentCount(Integer.valueOf(commentCount));
////
////                    // 处理高亮显示的结果
////                    HighlightField titleField = hit.getHighlightFields().get("title");
////                    if (titleField != null) {
////                        post.setTitle(titleField.getFragments()[0].toString());
////                    }
////
////                    HighlightField contentField = hit.getHighlightFields().get("content");
////                    if (contentField != null) {
////                        post.setContent(contentField.getFragments()[0].toString());
////                    }
////
////                    list.add(post);
////                }
////
////                return new AggregatedPageImpl(list, pageable,
////                        hits.getTotalHits(), response.getAggregations(), response.getScrollId(), hits.getMaxScore());
////            }
////        });
////
////        System.out.println(page.getTotalElements());
////        System.out.println(page.getTotalPages());
////        System.out.println(page.getNumber());
////        System.out.println(page.getSize());
////        for (DiscussPost post : page) {
////            System.out.println(post);
////        }
////    }
//
//}
