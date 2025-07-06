package com.team5.catdogeats.orders.external;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

import java.util.concurrent.TimeUnit;

/**
 * 배송 추적 API OpenFeign 설정
 * API 호출 타임아웃, 재시도, 로깅 등을 설정
 */
@Slf4j
@Configuration
@EnableRetry
public class DeliveryTrackingApiConfiguration {

    @Value("${smart-courier.api.timeout.connect-timeout-ms:5000}")
    private int connectTimeout;

    @Value("${smart-courier.api.timeout.read-timeout-ms:10000}")
    private int readTimeout;

    @Value("${smart-courier.api.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${smart-courier.api.retry.backoff-delay-ms:1000}")
    private long backoffDelay;

    /**
     * Feign 요청 옵션 설정
     * 스마트택배 API의 응답 시간을 고려한 타임아웃 설정
     */
    @Bean
    public Request.Options deliveryTrackingRequestOptions() {
        return new Request.Options(
                connectTimeout, TimeUnit.MILLISECONDS,
                readTimeout, TimeUnit.MILLISECONDS,
                true // followRedirects
        );
    }

    /**
     * Feign 재시도 설정
     * API 호출 실패 시 재시도 정책 설정
     */
    @Bean
    public Retryer deliveryTrackingRetryer() {
        return new Retryer.Default(
                backoffDelay,           // 초기 지연 시간
                backoffDelay * 2,       // 최대 지연 시간
                maxAttempts             // 최대 재시도 횟수
        );
    }

    /**
     * Feign 로거 레벨 설정
     * 개발 환경에서는 FULL, 운영 환경에서는 BASIC 로깅
     */
    @Bean
    public Logger.Level deliveryTrackingFeignLoggerLevel() {
        // 개발 환경에서만 상세 로깅
        String activeProfile = System.getProperty("spring.profiles.active", "dev");
        return activeProfile.contains("prod") ? Logger.Level.BASIC : Logger.Level.FULL;
    }

    /**
     * 배송 추적 API 전용 에러 디코더
     * API 응답 오류를 적절한 예외로 변환
     */
    @Bean
    public ErrorDecoder deliveryTrackingErrorDecoder() {
        return new DeliveryTrackingErrorDecoder();
    }

    /**
     * 배송 추적 API 에러 디코더 구현체
     */
    @Slf4j
    public static class DeliveryTrackingErrorDecoder implements ErrorDecoder {

        private final ErrorDecoder defaultErrorDecoder = new Default();

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            int status = response.status();
            String requestUrl = response.request().url();

            log.warn("배송 추적 API 호출 실패 - method: {}, status: {}, url: {}",
                    methodKey, status, requestUrl);

            return switch (status) {
                case 400 -> new DeliveryTrackingApiException(
                        "잘못된 요청입니다. 택배사 코드나 운송장 번호를 확인해주세요.");

                case 401 -> new DeliveryTrackingApiException(
                        "API 키가 유효하지 않습니다.");

                case 403 -> new DeliveryTrackingApiException(
                        "API 호출 권한이 없습니다. API 키 권한을 확인해주세요.");

                case 429 -> new DeliveryTrackingApiException(
                        "API 호출 제한을 초과했습니다. 잠시 후 다시 시도해주세요.");

                case 500 -> new DeliveryTrackingApiException(
                        "배송 추적 서버 오류입니다. 잠시 후 다시 시도해주세요.");

                case 503 -> new DeliveryTrackingApiException(
                        "배송 추적 서비스가 일시적으로 이용할 수 없습니다.");

                default -> {
                    log.error("예상치 못한 배송 추적 API 오류 - status: {}, method: {}",
                            status, methodKey);
                    yield defaultErrorDecoder.decode(methodKey, response);
                }
            };
        }
    }

    /**
     * 배송 추적 API 전용 예외 클래스
     */
    public static class DeliveryTrackingApiException extends RuntimeException {

        public DeliveryTrackingApiException(String message) {
            super(message);
        }

        public DeliveryTrackingApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Feign 클라이언트 로그 설정을 위한 Logger 빈
     */
    @Bean(name = "deliveryTrackingApiLogger")
    public feign.Logger deliveryTrackingApiLogger() {
        return new feign.Logger() {
            @Override
            protected void log(String configKey, String format, Object... args) {
                // 민감한 API 키 정보는 마스킹 처리
                String logMessage = String.format(format, args);
                if (logMessage.contains("t_key=")) {
                    logMessage = logMessage.replaceAll("t_key=[^&\\s]+", "t_key=***");
                }

                log.debug("[배송추적 API] {}: {}", configKey, logMessage);
            }
        };
    }

    /**
     * API 호출 성능 모니터링을 위한 인터셉터
     */
    @Bean
    public DeliveryTrackingApiInterceptor deliveryTrackingApiInterceptor() {
        return new DeliveryTrackingApiInterceptor();
    }

    /**
     * API 호출 인터셉터 구현체
     */
    @Slf4j
    public static class DeliveryTrackingApiInterceptor implements feign.RequestInterceptor {

        @Override
        public void apply(feign.RequestTemplate template) {
            // 요청 시작 시간 기록
            template.header("X-Request-Start-Time", String.valueOf(System.currentTimeMillis()));

            // User-Agent 설정
            template.header("User-Agent", "Catdogeats-DeliveryTracking/1.0");

            // 요청 로깅 (개발 환경에서만)
            String activeProfile = System.getProperty("spring.profiles.active", "dev");
            if (activeProfile.contains("dev")) {
                log.debug("배송 추적 API 요청 - URL: {}, Method: {}",
                        template.url(), template.method());
            }
        }
    }

    /**
     * 배송 추적 API 응답 시간 모니터링
     */
    @Bean
    public DeliveryTrackingApiResponseTimeMonitor responseTimeMonitor() {
        return new DeliveryTrackingApiResponseTimeMonitor();
    }

    /**
     * API 응답 시간 모니터링 구현체
     */
    @Slf4j
    public static class DeliveryTrackingApiResponseTimeMonitor {

        /**
         * API 호출 응답 시간 기록
         */
        public void recordResponseTime(String methodKey, long responseTimeMs) {
            if (responseTimeMs > 5000) { // 5초 이상인 경우 경고
                log.warn("배송 추적 API 응답 시간 지연 - method: {}, time: {}ms",
                        methodKey, responseTimeMs);
            } else {
                log.debug("배송 추적 API 응답 시간 - method: {}, time: {}ms",
                        methodKey, responseTimeMs);
            }
        }

        /**
         * API 호출 성공률 모니터링
         */
        public void recordApiCall(String methodKey, boolean success, Exception error) {
            if (success) {
                log.debug("배송 추적 API 호출 성공 - method: {}", methodKey);
            } else {
                log.warn("배송 추적 API 호출 실패 - method: {}, error: {}",
                        methodKey, error != null ? error.getMessage() : "Unknown");
            }
        }
    }
}