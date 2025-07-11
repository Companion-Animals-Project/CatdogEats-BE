package com.team5.catdogeats.batch.service;

import com.team5.catdogeats.batch.domain.SettlementBatchExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

/**
 * 정산 배치 실행 서비스
 * 동시성 제어와 배치 실행을 통합 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementBatchExecutionServiceImpl implements SettlementBatchExecutionService {

    private final JobLauncher jobLauncher;
    private final BatchConcurrencyService batchConcurrencyService;
    private final Job settlementChunkDailyJob;
    private final Job settlementChunkMonthlyJob;

    /**
     * 정산 일일 배치 실행 (동시성 제어 포함)
     */
    @Override
    public BatchExecutionResult executeChunkDailyJob() {
        String batchName = SettlementBatchExecutionStatus.BatchName.SETTLEMENT_CREATE.getValue();
        String executionId = generateExecutionId("daily");

        return executeBatchWithLock(batchName, executionId, settlementChunkDailyJob, "일일 정산 배치");
    }

    /**
     * 정산 월간 배치 실행 (동시성 제어 포함)
     */
    @Override
    public BatchExecutionResult executeChunkMonthlyJob() {
        String batchName = SettlementBatchExecutionStatus.BatchName.SETTLEMENT_COMPLETE.getValue();
        String executionId = generateExecutionId("monthly");

        return executeBatchWithLock(batchName, executionId, settlementChunkMonthlyJob, "월간 정산 완료 배치");
    }

    /**
     * 동시성 제어를 포함한 배치 실행
     */
    @Override
    public BatchExecutionResult executeBatchWithLock(String batchName, String executionId,
                                                     Job job, String jobDescription) {
        log.info("배치 실행 시작 - batchName: {}, executionId: {}, description: {}",
                batchName, executionId, jobDescription);

        // 1. 락 획득 시도
        boolean lockAcquired = batchConcurrencyService.tryAcquireLock(batchName, executionId);
        if (!lockAcquired) {
            log.warn("배치 실행 중단 - 다른 프로세스에서 실행중이거나 락 획득 실패: {}", batchName);
            return BatchExecutionResult.failed("배치가 이미 실행중이거나 락 획득에 실패했습니다.");
        }

        try {
            // 2. 배치 실행
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("executionId", executionId)
                    .addString("batchName", batchName)
                    .addString("description", jobDescription)
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(job, jobParameters);

            // 3. 배치 완료까지 대기
            while (jobExecution.isRunning()) {
                try {
                    Thread.sleep(1000); // 1초 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("배치 실행 중 인터럽트 발생", e);
                }
            }

            // 4. 실행 결과 확인
            BatchStatus batchStatus = jobExecution.getStatus();
            ExitStatus exitStatus = jobExecution.getExitStatus();

            log.info("배치 실행 완료 - batchName: {}, status: {}, exitCode: {}",
                    batchName, batchStatus, exitStatus.getExitCode());

            // 4. 성공/실패에 따른 락 해제
            if (BatchStatus.COMPLETED.equals(batchStatus)) {
                batchConcurrencyService.releaseLockAsCompleted(batchName);
                return BatchExecutionResult.success(jobExecution);
            } else {
                batchConcurrencyService.releaseLockAsFailed(batchName);
                return BatchExecutionResult.failed("배치 실행 실패: " + exitStatus.getExitDescription());
            }

        } catch (JobExecutionAlreadyRunningException e) {
            log.error("배치가 이미 실행중입니다 - batchName: {}", batchName, e);
            batchConcurrencyService.releaseLockAsFailed(batchName);
            return BatchExecutionResult.failed("배치가 이미 실행중입니다.");

        } catch (JobRestartException e) {
            log.error("배치 재시작 오류 - batchName: {}", batchName, e);
            batchConcurrencyService.releaseLockAsFailed(batchName);
            return BatchExecutionResult.failed("배치 재시작 오류: " + e.getMessage());

        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("배치가 이미 완료됨 - batchName: {}", batchName, e);
            batchConcurrencyService.releaseLockAsFailed(batchName);
            return BatchExecutionResult.failed("배치가 이미 완료되었습니다.");

        } catch (JobParametersInvalidException e) {
            log.error("잘못된 Job 파라미터 - batchName: {}", batchName, e);
            batchConcurrencyService.releaseLockAsFailed(batchName);
            return BatchExecutionResult.failed("잘못된 Job 파라미터: " + e.getMessage());

        } catch (Exception e) {
            log.error("배치 실행 중 예상치 못한 오류 - batchName: {}", batchName, e);
            batchConcurrencyService.releaseLockAsFailed(batchName);
            return BatchExecutionResult.failed("배치 실행 중 오류: " + e.getMessage());
        }
    }

    /**
     * 실행 ID 생성
     */
    @Override
    public String generateExecutionId(String prefix) {
        return prefix + "_" + System.currentTimeMillis();
    }

    /**
     * 배치 실행 결과 클래스
     */
    public static class BatchExecutionResult {
        private final boolean success;
        private final String message;
        private final JobExecution jobExecution;

        private BatchExecutionResult(boolean success, String message, JobExecution jobExecution) {
            this.success = success;
            this.message = message;
            this.jobExecution = jobExecution;
        }

        public static BatchExecutionResult success(JobExecution jobExecution) {
            return new BatchExecutionResult(true, "배치 실행 성공", jobExecution);
        }

        public static BatchExecutionResult failed(String message) {
            return new BatchExecutionResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public JobExecution getJobExecution() { return jobExecution; }

        // 실행 통계 정보 제공
        public String getExecutionSummary() {
            if (jobExecution == null) {
                return "실행 정보 없음 - " + message;
            }

            long totalRead = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getReadCount).sum();
            long totalWrite = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getWriteCount).sum();
            long totalSkip = jobExecution.getStepExecutions().stream()
                    .mapToLong(StepExecution::getSkipCount).sum();

            return String.format("처리 완료 - Read: %d, Write: %d, Skip: %d, 소요시간: %d초",
                    totalRead, totalWrite, totalSkip,
                    java.time.Duration.between(
                            jobExecution.getStartTime(),
                            jobExecution.getEndTime()
                    ).getSeconds());
        }
    }
}