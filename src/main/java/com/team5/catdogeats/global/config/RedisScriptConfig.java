package com.team5.catdogeats.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.io.IOException;

@Configuration
public class RedisScriptConfig {

    @Bean
    public RedisScript<String> rotateTokenScript() throws IOException {
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/RotateRefreshToken.lua"))
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

    @Bean
    public RedisScript<String> refreshTokenScript() throws IOException {
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource("lua/CreateRefreshToken.lua"))
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

}
