package com.team5.catdogeats.batch.scheduler;

import com.team5.catdogeats.batch.config.SettlementBatchProperties;
import com.team5.catdogeats.batch.service.BatchConcurrencyService;
import com.team5.catdogeats.batch.service.SettlementBatchExecutionService;
import com.team5.catdogeats.batch.service.SettlementBatchExecutionService.BatchExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정산 청크 배치 스케줄러
 * 기존 Tasklet 방식을 대체하는 성능 최적화된 청크 기반 배치 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "batch.settlement.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SettlementChunkBatchScheduler {

    private final SettlementBatchExecutionService batchExecutionService;
    private final BatchConcurrencyService batchConcurrencyService;
    private final SettlementBatchProperties batchProperties;

    /**
     * 정산 데이터 생성/갱신 청크 스케줄러
     * 설정 파일의 cron 표현식 사용
     */
    @Scheduled(cron = "${batch.settlement.daily-cron}")
    public void runChunkDailySettlementJob() {
        if (!batchProperties.isEnabled()) {
            log.info("정산 배치가 비활성화되어 있습니다. 스킵합니다.");
            return;
        }

        try {
            log.info("📊 정산 청크 일일 배치 작업 시작 - cron: ${batch.settlement.daily-cron}");
            log.info("배치 설정 - chunkSize: {}, skipLimit: {}, retryLimit: {}",
                    batchProperties.getChunkSize(),
                    batchProperties.getSkipLimit(),
                    batchProperties.getRetryLimit());

            long startTime = System.currentTimeMillis();

            // 일일 배치 실행
            BatchExecutionResult result = batchExecutionService.executeChunkDailyJob();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            if (result.isSuccess()) {
                log.info("✅ 정산 청크 일일 배치 작업 완료 - 실행시간: {}ms", executionTime);
                log.info("📈 실행 결과: {}", result.getExecutionSummary());

                // 성공 알림 (설정에 따라)
                if (batchProperties.getNotification().isSuccessNotificationEnabled()) {
                    sendSuccessNotification("일일 정산 배치", result);
                }
            } else {
                log.error("❌ 정산 청크 일일 배치 작업 실패 - 실행시간: {}ms, 원인: {}",
                        executionTime, result.getMessage());

                // 실패 알림
                if (batchProperties.getNotification().isFailureNotificationEnabled()) {
                    sendFailureNotification("일일 정산 배치", result);
                }
            }

        } catch (Exception e) {
            log.error("❌ 정산 청크 일일 배치 작업 중 예상치 못한 오류 발생", e);

            // 예외 발생 시 알림
            if (batchProperties.getNotification().isFailureNotificationEnabled()) {
                sendExceptionNotification("일일 정산 배치", e);
            }
        }
    }

    /**
     * 정산 완료 처리 청크 스케줄러
     * 설정 파일의 cron 표현식 사용
     */
    @Scheduled(cron = "${batch.settlement.monthly-cron}")
    public void runChunkMonthlySettlementJob() {
        if (!batchProperties.isEnabled()) {
            log.info("정산 배치가 비활성화되어 있습니다. 스킵합니다.");
            return;
        }

        try {
            log.info("💰 정산 청크 월간 완료 배치 작업 시작 - cron: ${batch.settlement.monthly-cron}");

            long startTime = System.currentTimeMillis();

            // 청크 기반 월간 배치 실행
            BatchExecutionResult result = batchExecutionService.executeChunkMonthlyJob();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            if (result.isSuccess()) {
                log.info("✅ 정산 청크 월간 완료 배치 작업 완료 - 실행시간: {}ms", executionTime);
                log.info("📈 실행 결과: {}", result.getExecutionSummary());

                // 월간 배치는 중요하므로 항상 성공 알림
                sendSuccessNotification("월간 정산 완료 배치", result);

            } else {
                log.error("❌ 정산 청크 월간 완료 배치 작업 실패 - 실행시간: {}ms, 원인: {}",
                        executionTime, result.getMessage());

                // 실패 알림
                sendFailureNotification("월간 정산 완료 배치", result);
            }

        } catch (Exception e) {
            log.error("❌ 정산 청크 월간 완료 배치 작업 중 예상치 못한 오류 발생", e);

            // 예외 발생 시 알림
            sendExceptionNotification("월간 정산 완료 배치", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 일일 청크 배치
     */
    public BatchExecutionResult runChunkDailyJobManually() {
        try {
            log.info("🔧 수동 정산 청크 일일 배치 작업 시작");

            BatchExecutionResult result = batchExecutionService.executeChunkDailyJob();

            if (result.isSuccess()) {
                log.info("✅ 수동 정산 청크 일일 배치 작업 완료");
                log.info("📈 실행 결과: {}", result.getExecutionSummary());
            } else {
                log.error("❌ 수동 정산 청크 일일 배치 작업 실패: {}", result.getMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("❌ 수동 정산 청크 일일 배치 작업 중 예외 발생", e);
            throw new RuntimeException("정산 청크 일일 배치 작업 실행 실패", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 월간 청크 배치
     */
    public BatchExecutionResult runChunkMonthlyJobManually() {
        try {
            log.info("🔧 수동 정산 청크 월간 완료 배치 작업 시작");

            BatchExecutionResult result = batchExecutionService.executeChunkMonthlyJob();

            if (result.isSuccess()) {
                log.info("✅ 수동 정산 청크 월간 완료 배치 작업 완료");
                log.info("📈 실행 결과: {}", result.getExecutionSummary());
            } else {
                log.error("❌ 수동 정산 청크 월간 완료 배치 작업 실패: {}", result.getMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("❌ 수동 정산 청크 월간 완료 배치 작업 중 예외 발생", e);
            throw new RuntimeException("정산 청크 월간 완료 배치 작업 실행 실패", e);
        }
    }

    /**
     * 타임아웃된 배치 정리 스케줄러 (매 30분마다 실행)
     */
    @Scheduled(fixedRate = 1800000) // 30분 = 30 * 60 * 1000ms
    public void cleanupTimeoutBatches() {
        try {
            log.debug("배치 타임아웃 정리 작업 시작");
            batchConcurrencyService.cleanupTimeoutBatches();
            log.debug("배치 타임아웃 정리 작업 완료");
        } catch (Exception e) {
            log.error("배치 타임아웃 정리 작업 중 오류 발생", e);
        }
    }

    /**
     * 배치 상태 체크 스케줄러 (매 10분마다 실행)
     */
    @Scheduled(fixedRate = 600000) // 10분 = 10 * 60 * 1000ms
    public void checkBatchStatus() {
        try {
            var runningBatches = batchConcurrencyService.getRunningBatches();

            if (!runningBatches.isEmpty()) {
                log.info("현재 실행중인 배치 - 건수: {}", runningBatches.size());

                runningBatches.forEach(batch -> {
                    long runningMinutes = java.time.Duration.between(
                            batch.getStartedAt().toLocalDateTime(),
                            java.time.LocalDateTime.now()
                    ).toMinutes();

                    log.info("실행중인 배치 - name: {}, 실행시간: {}분, 시작시간: {}",
                            batch.getBatchName(), runningMinutes, batch.getStartedAt());

                    // 설정된 임계치보다 오래 실행중인 경우 경고
                    if (runningMinutes > batchProperties.getNotification().getSlowProcessingThreshold() / 60) {
                        log.warn("⚠️ 배치 처리 시간 과다 - name: {}, 실행시간: {}분",
                                batch.getBatchName(), runningMinutes);
                    }
                });
            }

        } catch (Exception e) {
            log.error("배치 상태 체크 중 오류 발생", e);
        }
    }

    /**
     * 알림 메서드들 (향후 확장 가능)
     */
    private void sendSuccessNotification(String batchName, BatchExecutionResult result) {
        // TODO: 실제 알림 시스템 연동 (이메일, 슬랙 등)
        log.info("🎉 [{}] 배치 성공 알림 - {}", batchName, result.getExecutionSummary());
    }

    private void sendFailureNotification(String batchName, BatchExecutionResult result) {
        // TODO: 실제 알림 시스템 연동
        log.error("🚨 [{}] 배치 실패 알림 - {}", batchName, result.getMessage());
    }

    private void sendExceptionNotification(String batchName, Exception e) {
        // TODO: 실제 알림 시스템 연동
        log.error("🚨 [{}] 배치 예외 알림 - {}: {}", batchName, e.getClass().getSimpleName(), e.getMessage());
    }
}