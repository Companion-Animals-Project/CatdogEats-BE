package com.team5.catdogeats.batch.forecast.scheduler;

import com.team5.catdogeats.batch.forecast.config.ForecastBatchProperties;
import com.team5.catdogeats.batch.forecast.service.ForecastBatchConcurrencyService;
import com.team5.catdogeats.batch.forecast.service.ForecastBatchExecutionService;
import com.team5.catdogeats.batch.forecast.service.ForecastBatchExecutionService.BatchExecutionResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 수요예측 배치 스케줄러
 * 정산 배치와 동일한 패턴으로 구성
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "batch.forecast.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ForecastBatchScheduler {

    private final ForecastBatchExecutionService batchExecutionService;
    private final ForecastBatchConcurrencyService batchConcurrencyService;
    private final ForecastBatchProperties forecastBatchProperties;

    /**
     * 일별 판매 집계 스케줄러 (매일 새벽 2시)
     */
    @Scheduled(cron = "${batch.forecast.aggregation-cron}")
    public void runAggregationJob() {
        if (!forecastBatchProperties.isEnabled()) {
            log.info("수요예측 배치가 비활성화되어 있습니다. 스킵합니다.");
            return;
        }

        try {
            log.info("수요예측 일별 집계 배치 작업 시작 - cron: ${batch.forecast.aggregation-cron}");
            log.info("배치 설정 - chunkSize: {}, skipLimit: {}, retryLimit: {}",
                    forecastBatchProperties.getChunkSize(),
                    forecastBatchProperties.getSkipLimit(),
                    forecastBatchProperties.getRetryLimit());

            long startTime = System.currentTimeMillis();

            // 일별 집계 배치 실행
            BatchExecutionResult result = batchExecutionService.executeAggregationJob();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            if (result.isSuccess()) {
                log.info("수요예측 일별 집계 배치 작업 완료 - 실행시간: {}ms", executionTime);
                log.info("실행 결과: {}", result.getExecutionSummary());

                // 성공 알림 (설정에 따라)
                if (forecastBatchProperties.getNotification().isSuccessNotificationEnabled()) {
                    sendSuccessNotification("일별 집계", result.getExecutionSummary());
                }

            } else {
                log.error("수요예측 일별 집계 배치 작업 실패 - 실행시간: {}ms, 원인: {}",
                        executionTime, result.getMessage());

                // 실패 알림
                if (forecastBatchProperties.getNotification().isFailureNotificationEnabled()) {
                    sendFailureNotification("일별 집계", result.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("수요예측 일별 집계 배치 작업 중 예상치 못한 오류 발생", e);

            // 예외 발생시 알림
            if (forecastBatchProperties.getNotification().isFailureNotificationEnabled()) {
                sendFailureNotification("일별 집계", "예상치 못한 오류: " + e.getMessage());
            }
        }
    }

    /**
     * 수요예측 실행 스케줄러 (매일 새벽 3시)
     */
    @Scheduled(cron = "${batch.forecast.forecast-cron}")
    public void runPredictionJob() {
        if (!forecastBatchProperties.isEnabled()) {
            log.info("수요예측 배치가 비활성화되어 있습니다. 스킵합니다.");
            return;
        }

        try {
            log.info("수요예측 실행 배치 작업 시작 - cron: ${batch.forecast.forecast-cron}");

            long startTime = System.currentTimeMillis();

            // 수요예측 실행 배치 실행
            BatchExecutionResult result = batchExecutionService.executePredictionJob();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            if (result.isSuccess()) {
                log.info("수요예측 실행 배치 작업 완료 - 실행시간: {}ms", executionTime);
                log.info("실행 결과: {}", result.getExecutionSummary());

                // 성공 알림 (설정에 따라)
                if (forecastBatchProperties.getNotification().isSuccessNotificationEnabled()) {
                    sendSuccessNotification("수요예측 실행", result.getExecutionSummary());
                }

            } else {
                log.error("수요예측 실행 배치 작업 실패 - 실행시간: {}ms, 원인: {}",
                        executionTime, result.getMessage());

                // 실패 알림
                if (forecastBatchProperties.getNotification().isFailureNotificationEnabled()) {
                    sendFailureNotification("수요예측 실행", result.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("수요예측 실행 배치 작업 중 예상치 못한 오류 발생", e);

            // 예외 발생시 알림
            if (forecastBatchProperties.getNotification().isFailureNotificationEnabled()) {
                sendFailureNotification("수요예측 실행", "예상치 못한 오류: " + e.getMessage());
            }
        }
    }

    /**
     * 데이터 정리 스케줄러 (매주 일요일 새벽 4시)
     */
    @Scheduled(cron = "0 0 4 * * SUN") // 매주 일요일 새벽 4시
    public void runCleanupJob() {
        if (!forecastBatchProperties.isEnabled()) {
            log.info("수요예측 배치가 비활성화되어 있습니다. 스킵합니다.");
            return;
        }

        try {
            log.info("수요예측 데이터 정리 배치 작업 시작");

            long startTime = System.currentTimeMillis();

            // 데이터 정리 배치 실행
            BatchExecutionResult result = batchExecutionService.executeCleanupJob();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            if (result.isSuccess()) {
                log.info("수요예측 데이터 정리 배치 작업 완료 - 실행시간: {}ms", executionTime);
                log.info("실행 결과: {}", result.getExecutionSummary());

            } else {
                log.error("수요예측 데이터 정리 배치 작업 실패 - 실행시간: {}ms, 원인: {}",
                        executionTime, result.getMessage());

                // 실패 알림
                if (forecastBatchProperties.getNotification().isFailureNotificationEnabled()) {
                    sendFailureNotification("데이터 정리", result.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("수요예측 데이터 정리 배치 작업 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * 타임아웃된 배치 정리 스케줄러 (매 6시간마다 실행)
     */
    @Scheduled(fixedRate = 21600000) // 6시간
    public void cleanupTimeoutBatches() {
        try {
            log.debug("수요예측 배치 타임아웃 정리 작업 시작");
            batchConcurrencyService.cleanupTimeoutBatches();
            forceCleanupStuckBatches();
            log.debug("수요예측 배치 타임아웃 정리 작업 완료");

        } catch (Exception e) {
            log.error("수요예측 배치 타임아웃 정리 작업 중 오류 발생", e);
        }
    }

    /**
     * 애플리케이션 시작 시 즉시 정리
     */
    @PostConstruct
    public void cleanupOnStartup() {
        try {
            log.info("애플리케이션 시작 - 수요예측 배치 상태 초기화");

            // 배치 상태 테이블 초기화
            batchConcurrencyService.initializeBatchStatuses();

            // 비정상 종료된 배치들 강제 정리
            forceCleanupStuckBatches();

            log.info("수요예측 배치 상태 초기화 완료");

        } catch (Exception e) {
            log.error("수요예측 배치 상태 초기화 중 오류 발생", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 일별 집계 배치
     */
    public BatchExecutionResult runAggregationJobManually() {
        try {
            log.info("수동 수요예측 일별 집계 배치 작업 시작");

            BatchExecutionResult result = batchExecutionService.executeAggregationJob();

            if (result.isSuccess()) {
                log.info("수동 수요예측 일별 집계 배치 작업 완료");
                log.info("실행 결과: {}", result.getExecutionSummary());
            } else {
                log.error("수동 수요예측 일별 집계 배치 작업 실패: {}", result.getMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("수동 수요예측 일별 집계 배치 작업 중 예외 발생", e);
            throw new RuntimeException("수요예측 일별 집계 배치 작업 실행 실패", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 수요예측 실행 배치
     */
    public BatchExecutionResult runPredictionJobManually() {
        try {
            log.info("수동 수요예측 실행 배치 작업 시작");

            BatchExecutionResult result = batchExecutionService.executePredictionJob();

            if (result.isSuccess()) {
                log.info("수동 수요예측 실행 배치 작업 완료");
                log.info("실행 결과: {}", result.getExecutionSummary());
            } else {
                log.error("수동 수요예측 실행 배치 작업 실패: {}", result.getMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("수동 수요예측 실행 배치 작업 중 예외 발생", e);
            throw new RuntimeException("수요예측 실행 배치 작업 실행 실패", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 데이터 정리 배치
     */
    public BatchExecutionResult runCleanupJobManually() {
        try {
            log.info("수동 수요예측 데이터 정리 배치 작업 시작");

            BatchExecutionResult result = batchExecutionService.executeCleanupJob();

            if (result.isSuccess()) {
                log.info("수동 수요예측 데이터 정리 배치 작업 완료");
                log.info("실행 결과: {}", result.getExecutionSummary());
            } else {
                log.error("수동 수요예측 데이터 정리 배치 작업 실패: {}", result.getMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("수동 수요예측 데이터 정리 배치 작업 중 예외 발생", e);
            throw new RuntimeException("수요예측 데이터 정리 배치 작업 실행 실패", e);
        }
    }

    /**
     * 비정상 종료된 배치들 강제 정리
     */
    private void forceCleanupStuckBatches() {
        try {
            log.debug("비정상 종료된 수요예측 배치 강제 정리 시작");
            batchConcurrencyService.forceCleanupStuckBatches();
            log.debug("비정상 종료된 수요예측 배치 강제 정리 완료");

        } catch (Exception e) {
            log.error("비정상 종료된 수요예측 배치 강제 정리 중 오류 발생", e);
        }
    }

    /**
     * 성공 알림 전송 (추후 구현)
     */
    private void sendSuccessNotification(String batchType, String summary) {
        // TODO: 이메일, 슬랙 등 알림 서비스 연동
        log.info("📧 성공 알림 - 배치: {}, 결과: {}", batchType, summary);
    }

    /**
     * 실패 알림 전송 (추후 구현)
     */
    private void sendFailureNotification(String batchType, String errorMessage) {
        // TODO: 이메일, 슬랙 등 알림 서비스 연동
        log.error("🚨 실패 알림 - 배치: {}, 오류: {}", batchType, errorMessage);
    }
}