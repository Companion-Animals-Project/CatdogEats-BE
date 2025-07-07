package com.team5.catdogeats.orders.external;

import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 스마트택배 API Feign 클라이언트 설정
 * API 호출 시 필요한 인증, 타임아웃, 로깅, 오류 처리 설정
 */
@Slf4j
@Configuration
public class DeliveryTrackingApiConfiguration {

    @Value("${smart-courier.api.key}")
    private String apiKey;

    @Value("${smart-courier.api.timeout.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${smart-courier.api.timeout.read-timeout-ms:10000}")
    private int readTimeoutMs;

    /**
     * API 키 자동 추가 인터셉터
     * 모든 요청에 t_key 파라미터를 자동으로 추가
     */
    @Bean
    public RequestInterceptor apiKeyInterceptor() {
        return requestTemplate -> {
            // API 키를 쿼리 파라미터로 추가
            requestTemplate.query("t_key", apiKey);

            // 헤더 설정
            requestTemplate.header("Accept", "application/json");
            requestTemplate.header("Content-Type", "application/json; charset=UTF-8");

            log.debug("스마트택배 API 요청 - URL: {}, Method: {}",
                    requestTemplate.url(), requestTemplate.method());
        };
    }

    /**
     * 스마트택배 API 전용 오류 디코더
     * API 응답 코드에 따른 적절한 예외 처리
     */
    @Bean
    public ErrorDecoder smartCourierErrorDecoder() {
        return new SmartCourierErrorDecoder();
    }

    /**
     * Feign 로깅 레벨 설정
     * 개발 환경에서 API 호출 디버깅용
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC; // NONE, BASIC, HEADERS, FULL 중 선택
    }

    /**
     * 스마트택배 API 전용 오류 디코더 구현체
     */
    @Slf4j
    public static class SmartCourierErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultErrorDecoder = new Default();

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            String errorMessage = String.format("스마트택배 API 호출 실패 - 메서드: %s, 상태코드: %d",
                    methodKey, response.status());

            log.warn(errorMessage);

            return switch (response.status()) {
                case 400 -> new SmartCourierApiException("잘못된 요청입니다. 파라미터를 확인해주세요", response.status());
                case 401 -> new SmartCourierApiException("API 키가 유효하지 않습니다", response.status());
                case 403 -> new SmartCourierApiException("API 사용 권한이 없습니다", response.status());
                case 429 -> new SmartCourierApiException("API 호출 한도를 초과했습니다", response.status());
                case 500 -> new SmartCourierApiException("스마트택배 서버 오류입니다", response.status());
                case 503 -> new SmartCourierApiException("스마트택배 서비스가 일시적으로 사용할 수 없습니다", response.status());
                default -> defaultErrorDecoder.decode(methodKey, response);
            };
        }
    }

    /**
     * 스마트택배 API 전용 예외 클래스
     */
    @Getter
    public static class SmartCourierApiException extends RuntimeException {
        private final int statusCode;

        public SmartCourierApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        /**
         * 재시도 가능한 오류인지 확인
         * @return 재시도 가능한 오류면 true
         */
        public boolean isRetryable() {
            // 5xx 서버 오류는 재시도 가능
            return statusCode >= 500;
        }

        /**
         * API 제한 오류인지 확인
         * @return API 제한 오류면 true
         */
        public boolean isRateLimitError() {
            return statusCode == 429;
        }
    }
}