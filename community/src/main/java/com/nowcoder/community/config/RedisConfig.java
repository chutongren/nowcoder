package com.nowcoder.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory); //工厂设置给template

        //指定序列化的方式
        //设置key的序列化的方式
        template.setKeySerializer(RedisSerializer.string());
        //设置普通的value的序列化的方式
        template.setHashKeySerializer(RedisSerializer.json());
        //设置hash的key的序列化的方式
        template.setHashValueSerializer(RedisSerializer.json());
        //设置hash的value的序列化的方式
        template.setValueSerializer(RedisSerializer.json());

        // 做完设置后，触发一下，使其生效
        template.afterPropertiesSet();
        return template;
    }
}
