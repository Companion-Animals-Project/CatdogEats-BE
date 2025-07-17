package com.team5.catdogeats.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession
public class SessionConfig {
//    @Bean
//    public FilterRegistrationBean<SessionRepositoryFilter<?>> springSessionFilter(
//            SessionRepositoryFilter<?> springSessionRepositoryFilter) {
//
//        FilterRegistrationBean<SessionRepositoryFilter<?>> registration =
//                new FilterRegistrationBean<>(springSessionRepositoryFilter);
//
//        registration.addUrlPatterns("/v1/admins/**");
//
//        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
//
//        return registration;
//    }
}
