package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.batch.reader.SettlementCompleteItemReader;
import com.team5.catdogeats.batch.reader.SettlementCreateItemReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.context.ApplicationContext;

/**
 * 정산 배치 Step 실행 모니터링 리스너
 * Step 실행 전후 로깅 및 통계 수집
 */
@Slf4j
@RequiredArgsConstructor
public class SettlementStepExecutionListener implements StepExecutionListener {

    private final String stepType; // "정산생성" 또는 "정산완료"
    private final ApplicationContext applicationContext;

    private long startTime;

    /**
     * Step 실행 전 호출
     * ItemReader 상태 초기화
     */
    @Override
    public void beforeStep(StepExecution stepExecution) {
        startTime = System.currentTimeMillis();

        String stepName = stepExecution.getStepName();
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();

        log.info("[{}] 배치 Step 시작 - stepName: {}, jobName: {}", stepType, stepName, jobName);

        // ItemReader 상태 초기화
        try {
            resetItemReader(stepName);
        } catch (Exception e) {
            log.error("ItemReader 초기화 실패 - stepName: {}", stepName, e);
            // 초기화 실패해도 배치는 계속 진행 (로그만 남김)
        }
    }

    /**
     * Step 실행 후 호출
     * 실행 결과 로깅 및 성능 분석
     */
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        long executionSeconds = executionTime / 1000;

        // 실행 통계 수집
        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long skipCount = stepExecution.getSkipCount();
        long filterCount = stepExecution.getFilterCount();
        long commitCount = stepExecution.getCommitCount();
        long rollbackCount = stepExecution.getRollbackCount();

        // 기본 실행 결과 로깅
        log.info("[{}] 배치 Step 완료 - 실행시간: {}초, 처리건수: read={}, write={}, skip={}, filter={}, 트랜잭션: commit={}, rollback={}",
                stepType, executionSeconds, readCount, writeCount, skipCount, filterCount, commitCount, rollbackCount);

        // 성능 분석 (처리된 건수가 있는 경우에만)
        if (readCount > 0) {
            long throughput = readCount / Math.max(executionSeconds, 1); // 0으로 나누기 방지
            long avgChunkTime = executionTime / Math.max(commitCount, 1);

            log.info("[{}] 성능 분석 - 처리량: {}건/초, 평균 청크 처리시간: {}ms",
                    stepType, throughput, avgChunkTime);
        }

        // 오류 상황 체크
        if (rollbackCount > 0) {
            log.warn("[{}] 롤백 발생 - rollbackCount: {}", stepType, rollbackCount);
        }

        if (skipCount > 0) {
            log.warn("[{}] 스킵 발생 - skipCount: {}", stepType, skipCount);
        }

        // Step 실행 상태에 따른 처리
        ExitStatus exitStatus = stepExecution.getExitStatus();
        if (ExitStatus.FAILED.equals(exitStatus)) {
            log.error("[{}] 배치 Step 실패 - exitCode: {}, exitDescription: {}",
                    stepType, exitStatus.getExitCode(), exitStatus.getExitDescription());
        } else if (ExitStatus.COMPLETED.equals(exitStatus)) {
            log.info("[{}] 배치 Step 성공 완료", stepType);
        }

        return exitStatus;
    }

    /**
     * Step 이름에 따라 해당하는 ItemReader 초기화
     */
    private void resetItemReader(String stepName) {
        try {
            if ("createSettlementsChunkStep".equals(stepName)) {
                // 정산 생성 ItemReader 초기화
                SettlementCreateItemReader reader = applicationContext.getBean(SettlementCreateItemReader.class);
                reader.reset();
                log.info("SettlementCreateItemReader 상태 초기화 완료");

            } else if ("completeSettlementsChunkStep".equals(stepName)) {
                // 정산 완료 ItemReader 초기화
                SettlementCompleteItemReader reader = applicationContext.getBean(SettlementCompleteItemReader.class);
                reader.reset();
                log.info("SettlementCompleteItemReader 상태 초기화 완료");

            } else {
                log.debug("알 수 없는 Step 이름 - 초기화 스킵: {}", stepName);
            }
        } catch (Exception e) {
            log.error("ItemReader Bean 조회 또는 초기화 실패 - stepName: {}", stepName, e);
            throw e;
        }
    }
}