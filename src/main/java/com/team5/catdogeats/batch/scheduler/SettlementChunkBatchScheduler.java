package com.team5.catdogeats.batch.scheduler;

import com.team5.catdogeats.batch.config.SettlementBatchProperties;
import com.team5.catdogeats.batch.service.BatchConcurrencyService;
import com.team5.catdogeats.batch.service.SettlementBatchExecutionService;
import com.team5.catdogeats.batch.service.SettlementBatchExecutionServiceImpl.BatchExecutionResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정산 청크 배치 스케줄러
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
     * 정산 데이터 생성스케줄러
     */
    @Scheduled(cron = "${batch.settlement.daily-cron}")
    public void runChunkDailySettlementJob() {
        if (!batchProperties.isEnabled()) {
            log.info("정산 배치가 비활성화되어 있습니다. 스킵합니다.");
            return;
        }

        try {
            log.info("정산 청크 일일 배치 작업 시작 - cron: ${batch.settlement.daily-cron}");
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
                log.info("정산 청크 일일 배치 작업 완료 - 실행시간: {}ms", executionTime);
                log.info("실행 결과: {}", result.getExecutionSummary());

            } else {
                log.error("정산 청크 일일 배치 작업 실패 - 실행시간: {}ms, 원인: {}",
                        executionTime, result.getMessage());


            }

        } catch (Exception e) {
            log.error("정산 청크 일일 배치 작업 중 예상치 못한 오류 발생", e);

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
            log.info("정산 청크 월간 완료 배치 작업 시작 - cron: ${batch.settlement.monthly-cron}");

            long startTime = System.currentTimeMillis();

            // 청크 기반 월간 배치 실행
            BatchExecutionResult result = batchExecutionService.executeChunkMonthlyJob();

            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            if (result.isSuccess()) {
                log.info("정산 청크 월간 완료 배치 작업 완료 - 실행시간: {}ms", executionTime);
                log.info("실행 결과: {}", result.getExecutionSummary());



            } else {
                log.error("정산 청크 월간 완료 배치 작업 실패 - 실행시간: {}ms, 원인: {}",
                        executionTime, result.getMessage());

            }

        } catch (Exception e) {
            log.error(" 정산 청크 월간 완료 배치 작업 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 일일 청크 배치
     */
    public BatchExecutionResult runChunkDailyJobManually() {
        try {
            log.info("수동 정산 청크 일일 배치 작업 시작");

            BatchExecutionResult result = batchExecutionService.executeChunkDailyJob();

            if (result.isSuccess()) {
                log.info("수동 정산 청크 일일 배치 작업 완료");
                log.info("실행 결과: {}", result.getExecutionSummary());
            } else {
                log.error("수동 정산 청크 일일 배치 작업 실패: {}", result.getMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("수동 정산 청크 일일 배치 작업 중 예외 발생", e);
            throw new RuntimeException("정산 청크 일일 배치 작업 실행 실패", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 월간 청크 배치
     */
    public BatchExecutionResult runChunkMonthlyJobManually() {
        try {
            log.info("수동 정산 청크 월간 완료 배치 작업 시작");

            BatchExecutionResult result = batchExecutionService.executeChunkMonthlyJob();

            if (result.isSuccess()) {
                log.info("수동 정산 청크 월간 완료 배치 작업 완료");
                log.info("실행 결과: {}", result.getExecutionSummary());
            } else {
                log.error("수동 정산 청크 월간 완료 배치 작업 실패: {}", result.getMessage());
            }

            return result;

        } catch (Exception e) {
            log.error("수동 정산 청크 월간 완료 배치 작업 중 예외 발생", e);
            throw new RuntimeException("정산 청크 월간 완료 배치 작업 실행 실패", e);
        }
    }

    /**
     * 타임아웃된 배치 정리 스케줄러 (매 6시간마다 실행)
     */
    @Scheduled(fixedRate = 21600000) // 6시간
    public void cleanupTimeoutBatches() {
        try {
            log.debug("배치 타임아웃 정리 작업 시작");
            batchConcurrencyService.cleanupTimeoutBatches();
            forceCleanupStuckBatches();
            log.debug("배치 타임아웃 정리 작업 완료");

        } catch (Exception e) {
            log.error("배치 타임아웃 정리 작업 중 오류 발생", e);
        }
    }


    /**
     * 애플리케이션 시작 시 즉시 정리
     */
    @PostConstruct
    public void cleanupOnStartup() {
        try {
            log.info("애플리케이션 시작 - 배치 상태 즉시 정리 시작");

            // 5초 후에 정리 실행 (완전 초기화 대기)
            java.util.concurrent.CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS)
                    .execute(() -> {
                        try {
                            log.info("지연 정리 작업 시작");
                            forceCleanupStuckBatches();
                            log.info("지연 정리 작업 완료");
                        } catch (Exception e) {
                            log.error("지연 정리 작업 실패", e);
                        }
                    });

        } catch (Exception e) {
            log.error("애플리케이션 시작 시 배치 상태 정리 실패", e);
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
                        log.warn("배치 처리 시간 과다 - name: {}, 실행시간: {}분",
                                batch.getBatchName(), runningMinutes);
                    }
                });
            }

        } catch (Exception e) {
            log.error("배치 상태 체크 중 오류 발생", e);
        }
    }

    /**
     * 고착 상태 배치 강제 정리
     */
    private void forceCleanupStuckBatches() {
        try {
            var runningBatches = batchConcurrencyService.getRunningBatches();

            for (var batch : runningBatches) {
                long runningMinutes = java.time.Duration.between(
                        batch.getStartedAt().toLocalDateTime(),
                        java.time.LocalDateTime.now()
                ).toMinutes();

                // 2시간(120분) 이상 실행중인 배치는 강제 정리
                if (runningMinutes > 120) {
                    log.warn("고착 상태 배치 강제 정리 - name: {}, 실행시간: {}분",
                            batch.getBatchName(), runningMinutes);

                    batchConcurrencyService.forceReleaseLock(batch.getBatchName());

                    // 강제 정리 알림
                    log.error("강제 정리 완료 - batchName: {}, 실행시간: {}분 → IDLE로 변경",
                            batch.getBatchName(), runningMinutes);
                }
            }
        } catch (Exception e) {
            log.error("고착 상태 배치 강제 정리 중 오류", e);
        }
    }


}