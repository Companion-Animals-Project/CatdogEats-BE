package com.team5.catdogeats.batch.service;

import org.springframework.batch.core.Job;

public interface SettlementBatchExecutionService {
    SettlementBatchExecutionServiceImpl.BatchExecutionResult executeChunkDailyJob();

    SettlementBatchExecutionServiceImpl.BatchExecutionResult executeChunkMonthlyJob();

    SettlementBatchExecutionServiceImpl.BatchExecutionResult executeBatchWithLock(String batchName, String executionId,
                                                                                  Job job, String jobDescription);

    String generateExecutionId(String prefix);
}
