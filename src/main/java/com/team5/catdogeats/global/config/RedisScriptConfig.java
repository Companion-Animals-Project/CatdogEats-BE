package com.team5.catdogeats.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class RedisScriptConfig {

    @Bean
    public RedisScript<String> rotateTokenScript() throws IOException {
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(
                new String(new ClassPathResource("lua/RotateRefreshToken.lua").getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8)
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

    @Bean
    public RedisScript<String> refreshTokenScript() throws IOException {
        DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(
                new String(new ClassPathResource("lua/CreateRefreshToken.lua").getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8)
        );
        redisScript.setResultType(String.class);
        return redisScript;
    }

}
