package com.team5.catdogeats.batch.forecast.config;

import com.team5.catdogeats.batch.forecast.dto.ForecastBatchItem;
import com.team5.catdogeats.batch.mapper.ForecastBatchMapper;
import com.team5.catdogeats.batch.forecast.processor.ForecastBatchItemProcessor;
import com.team5.catdogeats.batch.forecast.reader.ForecastBatchItemReader;
import com.team5.catdogeats.batch.forecast.writer.ForecastBatchItemWriter;
import com.team5.catdogeats.forecast.service.DailySalesAggregationService;
import com.team5.catdogeats.forecast.service.DemandForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

/**
 * 수요예측 Spring Batch 설정
 * 정산 배치와 동일한 패턴으로 구성
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ForecastBatchConfig {

    private final JobRepository jobRepository;
    private final ForecastBatchMapper forecastBatchMapper;
    private final ForecastBatchProperties forecastBatchProperties;
    private final DemandForecastService demandForecastService;
    private final DailySalesAggregationService dailySalesAggregationService;

    @Qualifier("batchTransactionManager")
    private final PlatformTransactionManager batchTransactionManager;

    // ================================
    // Job 정의
    // ================================

    /**
     * 일별 판매 집계 배치 Job (매일 새벽 2시)
     */
    @Bean(name = "forecastAggregationJob")
    public Job forecastAggregationJob() {
        return new JobBuilder("forecastAggregationJob", jobRepository)
                .start(aggregationStep())
                .build();
    }

    /**
     * 수요예측 실행 배치 Job (매일 새벽 3시)
     */
    @Bean(name = "forecastPredictionJob")
    public Job forecastPredictionJob() {
        return new JobBuilder("forecastPredictionJob", jobRepository)
                .start(predictionStep())
                .build();
    }

    /**
     * 오래된 데이터 정리 배치 Job (주간 실행)
     */
    @Bean(name = "forecastCleanupJob")
    public Job forecastCleanupJob() {
        return new JobBuilder("forecastCleanupJob", jobRepository)
                .start(cleanupStep())
                .build();
    }

    // ================================
    // Step 정의
    // ================================

    /**
     * 일별 판매 집계 Step
     */
    @Bean
    public Step aggregationStep() {
        return new StepBuilder("aggregationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("일별 판매 집계 Tasklet 시작");

                    // 어제 날짜 집계 실행
                    LocalDate yesterday = LocalDate.now().minusDays(1);
                    int aggregatedRecords = dailySalesAggregationService.aggregateDailySales(yesterday);

                    log.info("일별 판매 집계 완료 - 날짜: {}, 처리 건수: {}", yesterday, aggregatedRecords);

                    // 처리 결과를 ExecutionContext에 저장
                    chunkContext.getStepContext().getStepExecution()
                            .getExecutionContext().putInt("aggregatedRecords", aggregatedRecords);

                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, batchTransactionManager)
                .build();
    }

    /**
     * 수요예측 실행 Step (청크 기반)
     */
    @Bean
    public Step predictionStep() {
        return new StepBuilder("predictionStep", jobRepository)
                .<ForecastBatchItem, ForecastBatchItem>chunk(forecastBatchProperties.getChunkSize(), batchTransactionManager)
                .reader(forecastBatchItemReader())
                .processor(forecastBatchItemProcessor())
                .writer(forecastBatchItemWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(forecastBatchProperties.getSkipLimit())
                .skipPolicy(forecastSkipPolicy())
                .retry(TransientDataAccessException.class)
                .retry(DeadlockLoserDataAccessException.class)
                .retryLimit(forecastBatchProperties.getRetryLimit())
                .listener(createStepExecutionListener())
                .build();
    }

    /**
     * 오래된 데이터 정리 Step
     */
    @Bean
    public Step cleanupStep() {
        return new StepBuilder("cleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("오래된 데이터 정리 Tasklet 시작");

                    LocalDate cutoffDate = LocalDate.now().minusDays(forecastBatchProperties.getCleanupOldDataDays());
                    int deletedCount = demandForecastService.cleanupOldForecasts(cutoffDate);

                    log.info("오래된 데이터 정리 완료 - 기준일: {}, 삭제 건수: {}", cutoffDate, deletedCount);

                    // 처리 결과를 ExecutionContext에 저장
                    chunkContext.getStepContext().getStepExecution()
                            .getExecutionContext().putInt("deletedRecords", deletedCount);

                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, batchTransactionManager)
                .build();
    }

    // ================================
    // ItemReader, ItemProcessor, ItemWriter 빈
    // ================================

    /**
     * 수요예측 ItemReader (@StepScope로 Step 실행마다 새 인스턴스)
     */
    @Bean
    @StepScope
    public ForecastBatchItemReader forecastBatchItemReader() {
        log.info("🆕 ForecastBatchItemReader 새 인스턴스 생성 - Step 스코프");
        return new ForecastBatchItemReader(forecastBatchMapper, forecastBatchProperties.getChunkSize());
    }

    /**
     * 수요예측 ItemProcessor
     */
    @Bean
    public ForecastBatchItemProcessor forecastBatchItemProcessor() {
        return new ForecastBatchItemProcessor(demandForecastService, forecastBatchProperties);
    }

    /**
     * 수요예측 ItemWriter
     */
    @Bean
    public ForecastBatchItemWriter forecastBatchItemWriter() {
        return new ForecastBatchItemWriter();
    }

    // ================================
    // Skip Policy 및 Listener 설정
    // ================================

    /**
     * 수요예측 전용 Skip Policy
     */
    @Bean
    public SkipPolicy forecastSkipPolicy() {
        return (Throwable t, long skipCount) -> {
            // 데이터 접근 오류는 재시도하지 말고 스킵
            if (t instanceof DataAccessException) {
                log.warn("데이터 접근 오류로 스킵 - skipCount: {}, error: {}", skipCount, t.getMessage());
                return true;
            }

            // 일반적인 비즈니스 로직 오류도 스킵 허용
            if (t instanceof IllegalArgumentException || t instanceof RuntimeException) {
                log.warn("비즈니스 로직 오류로 스킵 - skipCount: {}, error: {}", skipCount, t.getMessage());
                return true;
            }

            // 기타 예외는 스킵하지 않음
            log.error("스킵하지 않을 오류 - skipCount: {}, error: {}", skipCount, t.getMessage());
            return false;
        };
    }

    /**
     * Step 실행 리스너
     */
    @Bean
    public org.springframework.batch.core.StepExecutionListener createStepExecutionListener() {
        return new org.springframework.batch.core.StepExecutionListener() {
            @Override
            public void beforeStep(org.springframework.batch.core.StepExecution stepExecution) {
                log.info("=== 수요예측 Step 시작 ===");
                log.info("Step 이름: {}", stepExecution.getStepName());
                log.info("배치 설정 - chunkSize: {}, skipLimit: {}, retryLimit: {}",
                        forecastBatchProperties.getChunkSize(),
                        forecastBatchProperties.getSkipLimit(),
                        forecastBatchProperties.getRetryLimit());
            }

            @Override
            public org.springframework.batch.core.ExitStatus afterStep(org.springframework.batch.core.StepExecution stepExecution) {
                log.info("=== 수요예측 Step 완료 ===");
                log.info("Step 결과 - Read: {}, Write: {}, Skip: {}, 소요시간: {}초",
                        stepExecution.getReadCount(),
                        stepExecution.getWriteCount(),
                        stepExecution.getSkipCount(),
                        java.time.Duration.between(
                                stepExecution.getStartTime(),
                                stepExecution.getEndTime()
                        ).getSeconds());

                // Skip 비율이 높으면 경고
                if (stepExecution.getReadCount() > 0) {
                    double skipRate = (double) stepExecution.getSkipCount() / stepExecution.getReadCount() * 100;
                    if (skipRate > forecastBatchProperties.getNotification().getHighSkipRateThreshold()) {
                        log.warn("⚠️ 높은 Skip 비율 감지 - {:.1f}% (임계치: {:.1f}%)",
                                skipRate, forecastBatchProperties.getNotification().getHighSkipRateThreshold());
                    }
                }

                return stepExecution.getExitStatus();
            }
        };
    }

    /**
     * Job 실행 리스너
     */
    @Bean
    public org.springframework.batch.core.JobExecutionListener createJobExecutionListener() {
        return new org.springframework.batch.core.JobExecutionListener() {
            @Override
            public void beforeJob(org.springframework.batch.core.JobExecution jobExecution) {
                log.info("🚀 수요예측 배치 Job 시작 - {}", jobExecution.getJobInstance().getJobName());
                log.info("Job 파라미터: {}", jobExecution.getJobParameters());
            }

            @Override
            public void afterJob(org.springframework.batch.core.JobExecution jobExecution) {
                log.info("✅ 수요예측 배치 Job 완료 - {}", jobExecution.getJobInstance().getJobName());
                log.info("Job 상태: {}, 종료 코드: {}",
                        jobExecution.getStatus(), jobExecution.getExitStatus().getExitCode());

                if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
                    long executionSeconds = java.time.Duration.between(
                            jobExecution.getStartTime(), jobExecution.getEndTime()).getSeconds();
                    log.info("총 실행 시간: {}초", executionSeconds);

                    // 실행 시간이 임계치를 초과하면 경고
                    if (executionSeconds > forecastBatchProperties.getNotification().getSlowProcessingThreshold()) {
                        log.warn("⚠️ 느린 처리 시간 감지 - {}초 (임계치: {}초)",
                                executionSeconds, forecastBatchProperties.getNotification().getSlowProcessingThreshold());
                    }
                }
            }
        };
    }
}