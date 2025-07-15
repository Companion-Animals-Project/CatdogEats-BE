package com.team5.catdogeats.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenFeign 설정
 * Toss Payments API 통신을 위한 설정입니다.
 */
@Configuration
@EnableFeignClients(basePackages = "com.team5.catdogeats")
public class FeignConfig {
    // OpenFeign 활성화를 위한 설정 클래스
}