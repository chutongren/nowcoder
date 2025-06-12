package com.nowcoder.community.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsConfig {

    @Value("${spring.elasticsearch.uris}")
    private String esUrl;

    @Bean
    public RestHighLevelClient client() {
        // 解析 elasticsearch 地址
        String[] esHosts = esUrl.split(",");
        HttpHost[] httpHosts = new HttpHost[esHosts.length];

        for (int i = 0; i < esHosts.length; i++) {
            String[] hostParts = esHosts[i].split(":");
            httpHosts[i] = new HttpHost(hostParts[0], Integer.parseInt(hostParts[1]), "http");
        }

        // 使用 RestClient 创建 RestHighLevelClient
        return new RestHighLevelClient(RestClient.builder(httpHosts));
    }
}



//package com.nowcoder.community.config;
//
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.client.RestClient;
//import org.elasticsearch.client.RestClientBuilder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class EsConfig {
//
//    @Value("${spring.elasticsearch.uris}")
//    private String esUrl;
//
//    // localhost:9200 写在配置文件中,直接用 <- spring.elasticsearch.uris
//    @Bean
//    public RestHighLevelClient client() {
//        // 使用 RestClient 建立连接
//        RestClientBuilder builder = RestClient.builder(esUrl);
//        return new RestHighLevelClient(builder);
//    }
//}




//package com.nowcoder.community.config;
//
//import org.elasticsearch.client.RestHighLevelClient;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.elasticsearch.client.ClientConfiguration;
//import org.springframework.data.elasticsearch.client.erhlc.RestClients;
////import org.elasticsearch.client.RestClient;
//
///**
// * ClassName: EsConfig
// * Package: com.nowcoder.community.config
// * Description:
// * 实现模糊类 AbstractElasticsearchConfiguration
// * 来得到 RestHighLevelClient 用于查询
// *
// * @Autuor Dongjie Sang
// * @Create 2023/6/7 21:43
// * @Version 1.0
// */
//@Configuration
//public class EsConfig {
//    @Value("${spring.elasticsearch.uris}")
//    private String esUrl;
//
//    //localhost:9200 写在配置文件中,直接用 <- spring.elasticsearch.uris
//    @Bean
//    RestHighLevelClient client() {
//        ClientConfiguration clientConfiguration = ClientConfiguration.builder()
//                .connectedTo(esUrl)//elasticsearch地址
//                .build();
//
//        return RestClients.create(clientConfiguration).rest();
//    }
//}