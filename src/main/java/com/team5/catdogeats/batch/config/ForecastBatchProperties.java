package com.team5.catdogeats.batch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 수요예측 배치 설정 프로퍼티
 * application.yml에서 배치 관련 설정값들을 관리 - 여기 있는것은 yml설정 없을때 기본값
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "batch.forecast")
public class ForecastBatchProperties {

    /**
     * 배치 활성화 여부
     */
    private boolean enabled = true;

    /**
     * 일별 집계 배치 크론 표현식 (매일 새벽 2시)
     */
    private String aggregationCron = "0 0 2 * * ?";

    /**
     * 수요예측 배치 크론 표현식 (매일 새벽 3시)
     */
    private String forecastCron = "0 0 3 * * ?";

    /**
     * 청크 사이즈 (판매자 단위로 처리)
     */
    private int chunkSize = 100;

    /**
     * Skip 한계치
     */
    private int skipLimit = 50;

    /**
     * 재시도 한계치
     */
    private int retryLimit = 3;

    /**
     * 예측 기간 (일)
     */
    private int predictionPeriodDays = 7;

    /**
     * 과거 데이터 분석 기간 (일)
     */
    private int historicalDataDays = 30;

    /**
     * 최소 판매 기록 일수 (예측 가능 조건)
     */
    private int minSalesDataDays = 15;

    /**
     * 이동평균 윈도우 크기
     */
    private int movingAverageWindow = 7;

    /**
     * 배치 타임아웃 시간 (분)
     */
    private int timeoutMinutes = 120;

    /**
     * 성능 모니터링 활성화
     */
    private boolean performanceMonitoringEnabled = true;

    /**
     * 상세 로깅 활성화
     */
    private boolean detailLoggingEnabled = false;

    /**
     * 오래된 데이터 정리 기간 (일)
     */
    private int cleanupOldDataDays = 30;

    /**
     * 알림 설정
     */
    private NotificationConfig notification = new NotificationConfig();

    /**
     * 알림 설정 내부 클래스
     */
    @Getter
    @Setter
    public static class NotificationConfig {
        /**
         * 실패 시 알림 활성화
         */
        private boolean failureNotificationEnabled = true;

        /**
         * 성공 시 알림 활성화
         */
        private boolean successNotificationEnabled = false;

        /**
         * 높은 Skip 비율 임계치 (%)
         */
        private double highSkipRateThreshold = 5.0;

        /**
         * 느린 처리 임계치 (초)
         */
        private long slowProcessingThreshold = 7200; // 2시간

        /**
         * 낮은 신뢰도 임계치
         */
        private double lowConfidenceThreshold = 0.3;
    }

    /**
     * 설정값 유효성 검증
     */
    public void validate() {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize는 0보다 커야 합니다.");
        }
        if (skipLimit < 0) {
            throw new IllegalArgumentException("skipLimit은 0 이상이어야 합니다.");
        }
        if (retryLimit < 0) {
            throw new IllegalArgumentException("retryLimit은 0 이상이어야 합니다.");
        }
        if (predictionPeriodDays <= 0) {
            throw new IllegalArgumentException("predictionPeriodDays는 0보다 커야 합니다.");
        }
        if (historicalDataDays <= 0) {
            throw new IllegalArgumentException("historicalDataDays는 0보다 커야 합니다.");
        }
        if (minSalesDataDays <= 0) {
            throw new IllegalArgumentException("minSalesDataDays는 0보다 커야 합니다.");
        }
        if (movingAverageWindow <= 0) {
            throw new IllegalArgumentException("movingAverageWindow는 0보다 커야 합니다.");
        }
        if (timeoutMinutes <= 0) {
            throw new IllegalArgumentException("timeoutMinutes는 0보다 커야 합니다.");
        }
        if (cleanupOldDataDays <= 0) {
            throw new IllegalArgumentException("cleanupOldDataDays는 0보다 커야 합니다.");
        }
    }
}