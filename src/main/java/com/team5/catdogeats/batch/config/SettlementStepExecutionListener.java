package com.team5.catdogeats.batch.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 정산 배치 Step 실행 리스너
 * 각 Step의 실행 상황을 모니터링하고 로깅
 */
@Slf4j
@RequiredArgsConstructor
public class SettlementStepExecutionListener implements StepExecutionListener {

    private final String stepName;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        log.info("🚀 [{}] 배치 Step 시작 - stepName: {}, jobName: {}",
                stepName, stepExecution.getStepName(), stepExecution.getJobExecution().getJobInstance().getJobName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {

        // 실행 시간 계산
        LocalDateTime startTime = stepExecution.getStartTime();
        LocalDateTime endTime = stepExecution.getEndTime();
        Duration duration = Duration.between(startTime, endTime);

        // 처리 통계
        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long skipCount = stepExecution.getSkipCount();
        long filterCount = stepExecution.getFilterCount();

        // 커밋 통계
        long commitCount = stepExecution.getCommitCount();
        long rollbackCount = stepExecution.getRollbackCount();

        // 실행 상태
        ExitStatus exitStatus = stepExecution.getExitStatus();

        // 성공/실패 여부에 따른 로깅
        if (ExitStatus.COMPLETED.equals(exitStatus)) {
            log.info("✅ [{}] 배치 Step 완료 - " +
                            "실행시간: {}초, " +
                            "처리건수: read={}, write={}, skip={}, filter={}, " +
                            "트랜잭션: commit={}, rollback={}",
                    stepName,
                    duration.getSeconds(),
                    readCount, writeCount, skipCount, filterCount,
                    commitCount, rollbackCount);

            // 성능 분석 로그
            if (duration.getSeconds() > 0) {
                long throughput = readCount / duration.getSeconds();
                log.info("📊 [{}] 성능 분석 - 처리량: {}건/초, 평균 청크 처리시간: {}ms",
                        stepName, throughput, commitCount > 0 ? duration.toMillis() / commitCount : 0);
            }

        } else if (ExitStatus.FAILED.equals(exitStatus)) {
            log.error("❌ [{}] 배치 Step 실패 - " +
                            "실행시간: {}초, " +
                            "처리건수: read={}, write={}, skip={}, " +
                            "실패원인: {}",
                    stepName,
                    duration.getSeconds(),
                    readCount, writeCount, skipCount,
                    getFailureReason(stepExecution));

        } else {
            log.warn("⚠️ [{}] 배치 Step 비정상 종료 - 상태: {}, " +
                            "실행시간: {}초, " +
                            "처리건수: read={}, write={}, skip={}",
                    stepName, exitStatus.getExitCode(),
                    duration.getSeconds(),
                    readCount, writeCount, skipCount);
        }

        // Skip이 많이 발생한 경우 경고
        if (skipCount > 0) {
            double skipRate = (double) skipCount / (readCount > 0 ? readCount : 1) * 100;
            if (skipRate > 5.0) { // Skip 비율이 5% 초과 시 경고
                log.warn("⚠️ [{}] 높은 Skip 비율 감지 - Skip 건수: {}, 전체 대비: {:.2f}%",
                        stepName, skipCount, skipRate);
            }
        }

        // Rollback이 발생한 경우 경고
        if (rollbackCount > 0) {
            log.warn("⚠️ [{}] Rollback 발생 - 횟수: {}, Commit 대비: {:.2f}%",
                    stepName, rollbackCount,
                    commitCount > 0 ? (double) rollbackCount / commitCount * 100 : 0);
        }

        return exitStatus;
    }

    /**
     * Step 실패 원인 추출
     */
    private String getFailureReason(StepExecution stepExecution) {
        if (!stepExecution.getFailureExceptions().isEmpty()) {
            Throwable lastException = stepExecution.getFailureExceptions().get(
                    stepExecution.getFailureExceptions().size() - 1);
            return lastException.getClass().getSimpleName() + ": " + lastException.getMessage();
        }
        return "알 수 없는 오류";
    }
}