package com.team5.catdogeats.batch.service;

import com.team5.catdogeats.batch.domain.SettlementBatchExecutionStatus;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface BatchConcurrencyService {
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    boolean tryAcquireLock(String batchName, String executionId);

    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    void releaseLockAsCompleted(String batchName);

    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    void releaseLockAsFailed(String batchName);

    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    void forceReleaseLock(String batchName);

    @MybatisTransactional(readOnly = true)
    List<SettlementBatchExecutionStatus> getRunningBatches();

    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    void cleanupTimeoutBatches();

    @MybatisTransactional(readOnly = true)
    Optional<SettlementBatchExecutionStatus> getBatchStatus(String batchName);

    long calculateRunningMinutes(OffsetDateTime startedAt);
}
