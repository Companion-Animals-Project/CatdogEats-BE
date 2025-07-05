package com.team5.catdogeats.batch.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * 정산 배치 설정 프로퍼티
 * application.yml에서 배치 관련 설정값들을 관리
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "batch.settlement")
public class SettlementBatchProperties {

    /**
     * 배치 활성화 여부
     */
    private boolean enabled = true;

    /**
     * 일일 배치 크론 표현식
     */
    private String dailyCron = "0 0 2 * * ?";

    /**
     * 월간 배치 크론 표현식
     */
    private String monthlyCron = "0 0 4 1 * ?";

    /**
     * 청크 사이즈
     */
    private int chunkSize = 1000;

    /**
     * Skip 한계치
     */
    private int skipLimit = 100;

    /**
     * 재시도 한계치
     */
    private int retryLimit = 3;

    /**
     * 수수료율 (기본 10%)
     */
    private BigDecimal commissionRate = new BigDecimal("0.1");

    /**
     * 정산 확정 대기 기간 (일)
     */
    private int confirmationPeriodDays = 7;

    /**
     * 배치 타임아웃 시간 (분)
     */
    private int timeoutMinutes = 60;

    /**
     * 성능 모니터링 활성화
     */
    private boolean performanceMonitoringEnabled = true;

    /**
     * 상세 로깅 활성화
     */
    private boolean detailLoggingEnabled = false;

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
        private long slowProcessingThreshold = 3600; // 1시간
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
        if (commissionRate == null || commissionRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("commissionRate는 0 이상이어야 합니다.");
        }
        if (confirmationPeriodDays <= 0) {
            throw new IllegalArgumentException("confirmationPeriodDays는 0보다 커야 합니다.");
        }
        if (timeoutMinutes <= 0) {
            throw new IllegalArgumentException("timeoutMinutes는 0보다 커야 합니다.");
        }
    }
}