package com.team5.catdogeats.forecast.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 수요예측 배치 설정 프로퍼티
 */
@Data
@Component
@ConfigurationProperties(prefix = "batch.demand-forecast")
public class DemandForecastBatchProperties {

    /**
     * 배치 활성화 여부
     */
    private boolean enabled = true;

    /**
     * 일별 집계 + 수요예측 스케줄러 cron 표현식 (매일 새벽 3시)
     */
    private String dailyCron = "0 0 3 * * ?";

    /**
     * 청크 크기 (한 번에 처리할 판매자 수)
     */
    private int chunkSize = 50;

    /**
     * 병렬 처리 스레드 수
     */
    private int threadPoolSize = 5;

    /**
     * 스킵 허용 횟수 (개별 판매자 처리 실패 허용)
     */
    private int skipLimit = 10;

    /**
     * 재시도 횟수 (일시적 오류 재시도)
     */
    private int retryLimit = 3;

    /**
     * 배치 실행 타임아웃 (분)
     */
    private int timeoutMinutes = 120;

    /**
     * 오래된 예측 데이터 정리 기간 (일)
     */
    private int cleanupDays = 30;

    /**
     * 일별 집계 히스토리 보관 기간 (일)
     */
    private int aggregationHistoryDays = 90;
}