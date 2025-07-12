package com.team5.catdogeats.batch.scheduler;

import com.team5.catdogeats.batch.config.SettlementBatchProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정산 배치 스케줄러
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

    private final JobLauncher jobLauncher;
    private final Job settlementChunkDailyJob;
    private final Job settlementChunkMonthlyJob;
    private final SettlementBatchProperties batchProperties;

    /**
     * 정산 데이터 생성 스케줄러 (매일 실행)
     */
    @Scheduled(cron = "${batch.settlement.daily-cron}")
    public void runDailySettlementJob() {
        if (!batchProperties.isEnabled()) {
            log.info("정산 배치가 비활성화되어 있습니다. 스킵합니다.");
            return;
        }

        log.info("정산 일일 배치 작업 시작");

        try {
            // JobParameters 생성
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // Spring Batch Job 실행
            JobExecution jobExecution = jobLauncher.run(settlementChunkDailyJob, jobParameters);

            // 단순히 실행 시작 로그만 남김 (상태 판단 제거)
            log.info("정산 일일 배치 실행 요청 완료 - JobExecutionId: {}", jobExecution.getId());

            // 실제 완료는 Spring Batch가 별도 스레드에서 로깅함

        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("정산 일일 배치가 이미 실행중입니다", e);
        } catch (JobRestartException e) {
            log.error("정산 일일 배치 재시작 오류", e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("정산 일일 배치가 이미 완료되었습니다", e);
        } catch (JobParametersInvalidException e) {
            log.error("정산 일일 배치 파라미터 오류", e);
        } catch (Exception e) {
            log.error("정산 일일 배치 작업 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * 정산 완료 처리 스케줄러 (매월 실행)
     */
    @Scheduled(cron = "${batch.settlement.monthly-cron}")
    public void runMonthlySettlementJob() {
        if (!batchProperties.isEnabled()) {
            log.info("정산 배치가 비활성화되어 있습니다. 스킵합니다.");
            return;
        }

        log.info("정산 월간 완료 배치 작업 시작");

        try {
            // JobParameters 생성
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            // Spring Batch Job 실행
            JobExecution jobExecution = jobLauncher.run(settlementChunkMonthlyJob, jobParameters);

            log.info("정산 월간 완료 배치 실행 요청 완료 - JobExecutionId: {}", jobExecution.getId());


        } catch (JobExecutionAlreadyRunningException e) {
            log.warn("정산 월간 배치가 이미 실행중입니다", e);
        } catch (JobRestartException e) {
            log.error("정산 월간 배치 재시작 오류", e);
        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("정산 월간 배치가 이미 완료되었습니다", e);
        } catch (JobParametersInvalidException e) {
            log.error("정산 월간 배치 파라미터 오류", e);
        } catch (Exception e) {
            log.error("정산 월간 완료 배치 작업 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 일일 배치
     * Admin Controller에서 사용 (동기 처리 필요)
     */
    public JobExecution runDailyJobManually() {
        log.info("수동 정산 일일 배치 작업 시작");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("trigger", "manual") // 수동 실행 구분용
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(settlementChunkDailyJob, jobParameters);

            log.info("수동 정산 일일 배치 실행 요청 완료 - JobExecutionId: {}", jobExecution.getId());

            return jobExecution;

        } catch (Exception e) {
            log.error("수동 정산 일일 배치 작업 중 예외 발생", e);
            throw new RuntimeException("정산 일일 배치 작업 실행 실패", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 월간 배치
     * Admin Controller에서 사용 (동기 처리 필요)
     */
    public JobExecution runMonthlyJobManually() {
        log.info("수동 정산 월간 완료 배치 작업 시작");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("trigger", "manual") // 수동 실행 구분용
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(settlementChunkMonthlyJob, jobParameters);

            log.info("수동 정산 월간 완료 배치 실행 요청 완료 - JobExecutionId: {}", jobExecution.getId());

            return jobExecution;

        } catch (Exception e) {
            log.error("수동 정산 월간 완료 배치 작업 중 예외 발생", e);
            throw new RuntimeException("정산 월간 완료 배치 작업 실행 실패", e);
        }
    }
}