package com.team5.catdogeats.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession
public class SessionConfig {
//    @Bean
//    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.activateDefaultTyping(
//                LaissezFaireSubTypeValidator.instance,
//                ObjectMapper.DefaultTyping.NON_FINAL,
//                JsonTypeInfo.As.PROPERTY
//        );
//
//        return new GenericJackson2JsonRedisSerializer(mapper);
//    }
}
