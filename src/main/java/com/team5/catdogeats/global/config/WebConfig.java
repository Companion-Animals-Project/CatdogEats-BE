package com.team5.catdogeats.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 설정 클래스 - CORS 설정 포함
 * 프론트엔드(포트 5173)와 백엔드(포트 8080) 간의 통신을 위한 CORS 설정
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 프론트엔드 개발 서버 허용
                .allowedOrigins(
                        "http://localhost:5173",  // Vite 개발 서버
                        "http://127.0.0.1:5173",  // 대체 로컬 주소
                        "http://localhost:3000",  // React 개발 서버 (필요시)
                        "https://your-frontend-domain.com" // 운영 도메인 (필요시 수정)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // preflight 캐시 시간 (1시간)
    }
}