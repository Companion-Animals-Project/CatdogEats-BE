package com.team5.catdogeats.batch.service;

import com.team5.catdogeats.batch.ForecastBatchExecutionStatus;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.List;
import java.util.Optional;

public interface ForecastBatchConcurrencyService {
    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    boolean tryAcquireLock(String batchName, String executionId);

    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    void releaseLockAsCompleted(String batchName);

    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    void releaseLockAsFailed(String batchName);

    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    void forceReleaseLock(String batchName);

    @MybatisTransactional(readOnly = true)
    List<ForecastBatchExecutionStatus> getCurrentRunningBatches();

    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    void cleanupTimeoutBatches();

    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    void initializeBatchStatuses();

    @MybatisTransactional(propagation = Propagation.REQUIRES_NEW)
    void forceCleanupStuckBatches();

    @MybatisTransactional(readOnly = true)
    Optional<ForecastBatchExecutionStatus> getBatchStatus(String batchName);

    @MybatisTransactional(readOnly = true)
    List<ForecastBatchExecutionStatus> getAllBatchStatuses();
}
