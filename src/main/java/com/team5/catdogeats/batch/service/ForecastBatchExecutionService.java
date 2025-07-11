package com.team5.catdogeats.batch.service;

import com.team5.catdogeats.batch.service.impl.ForecastBatchExecutionServiceImpl;

public interface ForecastBatchExecutionService {
    ForecastBatchExecutionServiceImpl.BatchExecutionResult executeAggregationJob();

    ForecastBatchExecutionServiceImpl.BatchExecutionResult executePredictionJob();

    ForecastBatchExecutionServiceImpl.BatchExecutionResult executeCleanupJob();
}
