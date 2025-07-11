package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.batch.dto.ForecastBatchItem;
import com.team5.catdogeats.batch.mapper.ForecastBatchMapper;
import com.team5.catdogeats.batch.processor.ForecastBatchItemProcessor;
import com.team5.catdogeats.batch.reader.ForecastBatchItemReader;
import com.team5.catdogeats.batch.writer.ForecastBatchItemWriter;
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
 * DemandForecastService와 DailySalesAggregationService를 활용
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ForecastBatchJobConfig {

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
     * 일별 판매 집계 배치 Job
     */
    @Bean(name = "forecastAggregationJob")
    public Job forecastAggregationJob() {
        return new JobBuilder("forecastAggregationJob", jobRepository)
                .start(aggregationStep())
                .listener(createJobExecutionListener())
                .build();
    }

    /**
     * 수요예측 실행 배치 Job
     */
    @Bean(name = "forecastPredictionJob")
    public Job forecastPredictionJob() {
        return new JobBuilder("forecastPredictionJob", jobRepository)
                .start(predictionStep())
                .listener(createJobExecutionListener())
                .build();
    }

    /**
     * 오래된 데이터 정리 배치 Job (주간 실행)
     */
    @Bean(name = "forecastCleanupJob")
    public Job forecastCleanupJob() {
        return new JobBuilder("forecastCleanupJob", jobRepository)
                .start(cleanupStep())
                .listener(createJobExecutionListener())
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

                    // 집계 결과가 0개면 경고 로그
                    if (aggregatedRecords == 0) {
                        log.warn("집계된 판매 데이터가 없습니다. 어제({}) 주문이 없었거나 이미 집계되었을 수 있습니다.", yesterday);
                    }

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
     * 오래된 데이터 정리 Step //30일 이상된 데이터 삭제
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
     * DemandForecastService를 주입하여 실제 비즈니스 로직 실행
     */
    @Bean
    public ForecastBatchItemProcessor forecastBatchItemProcessor() {
        return new ForecastBatchItemProcessor(demandForecastService, forecastBatchProperties);
    }

    /**
     * 수요예측 ItemWriter (통계 수집 및 로깅)
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
     * 비즈니스 로직 검증 실패는 스킵, 시스템 오류는 재시도 후 스킵
     */
    @Bean
    public SkipPolicy forecastSkipPolicy() {
        return (Throwable t, long skipCount) -> {
            // 데이터 접근 오류는 재시도하지 말고 스킵
            if (t instanceof DataAccessException) {
                log.warn("데이터 접근 오류로 스킵 - skipCount: {}, error: {}", skipCount, t.getMessage());
                return true;
            }

            // 비즈니스 로직 검증 실패 (IllegalArgumentException)는 스킵
            if (t instanceof IllegalArgumentException) {
                log.warn("비즈니스 검증 실패로 스킵 - skipCount: {}, error: {}", skipCount, t.getMessage());
                return true;
            }

            // 일반적인 런타임 오류도 스킵 허용 (데이터 품질 문제 등)
            if (t instanceof RuntimeException) {
                log.warn("런타임 오류로 스킵 - skipCount: {}, error: {}", skipCount, t.getMessage());
                return true;
            }

            // 기타 예외는 스킵하지 않음 (시스템 중단)
            log.error("스킵하지 않을 심각한 오류 - skipCount: {}, error: {}", skipCount, t.getMessage());
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
                log.info("예측 설정 - 예측기간: {}일, 과거데이터: {}일, 최소판매일수: {}일",
                        forecastBatchProperties.getPredictionPeriodDays(),
                        forecastBatchProperties.getHistoricalDataDays(),
                        forecastBatchProperties.getMinSalesDataDays());
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

                // Skip 비율 분석
                if (stepExecution.getReadCount() > 0) {
                    double skipRate = (double) stepExecution.getSkipCount() / stepExecution.getReadCount() * 100;
                    double successRate = (double) stepExecution.getWriteCount() / stepExecution.getReadCount() * 100;

                    log.info("처리 통계 - 성공률: {:.1f}%, Skip률: {:.1f}%", successRate, skipRate);

                    if (skipRate > forecastBatchProperties.getNotification().getHighSkipRateThreshold()) {
                        log.warn("높은 Skip 비율 감지 - {:.1f}% (임계치: {:.1f}%)",
                                skipRate, forecastBatchProperties.getNotification().getHighSkipRateThreshold());
                    }
                }

                return stepExecution.getExitStatus();
            }
        };
    }

    /**
     * Job 실행 리스너 (향상된 모니터링)
     */
    @Bean
    public org.springframework.batch.core.JobExecutionListener createJobExecutionListener() {
        return new org.springframework.batch.core.JobExecutionListener() {
            @Override
            public void beforeJob(org.springframework.batch.core.JobExecution jobExecution) {
                log.info("수요예측 배치 Job 시작 - {}", jobExecution.getJobInstance().getJobName());
                log.info("Job 파라미터: {}", jobExecution.getJobParameters());
                log.info("실행 시간: {}", java.time.LocalDateTime.now());
            }

            @Override
            public void afterJob(org.springframework.batch.core.JobExecution jobExecution) {
                log.info("수요예측 배치 Job 완료 - {}", jobExecution.getJobInstance().getJobName());
                log.info("Job 상태: {}, 종료 코드: {}",
                        jobExecution.getStatus(), jobExecution.getExitStatus().getExitCode());

                if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
                    long executionSeconds = java.time.Duration.between(
                            jobExecution.getStartTime(), jobExecution.getEndTime()).getSeconds();
                    log.info("총 실행 시간: {}초", executionSeconds);

                    // 실행 시간 임계치 확인
                    if (executionSeconds > forecastBatchProperties.getNotification().getSlowProcessingThreshold()) {
                        log.warn("느린 처리 시간 감지 - {}초 (임계치: {}초)",
                                executionSeconds, forecastBatchProperties.getNotification().getSlowProcessingThreshold());
                    }
                }

                // Job 실행 통계 출력
                printJobExecutionSummary(jobExecution);
            }
        };
    }

    /**
     * Job 실행 요약 출력
     */
    private void printJobExecutionSummary(org.springframework.batch.core.JobExecution jobExecution) {
        log.info("========================================");
        log.info("수요예측 배치 Job 실행 요약");
        log.info("========================================");

        jobExecution.getStepExecutions().forEach(stepExecution -> {
            String stepName = stepExecution.getStepName();

            if ("aggregationStep".equals(stepName)) {
                int aggregatedRecords = stepExecution.getExecutionContext().getInt("aggregatedRecords", 0);
                log.info("일별 집계: {}건", aggregatedRecords);

            } else if ("predictionStep".equals(stepName)) {
                log.info("수요예측: Read {}건, Write {}건, Skip {}건",
                        stepExecution.getReadCount(),
                        stepExecution.getWriteCount(),
                        stepExecution.getSkipCount());

            } else if ("cleanupStep".equals(stepName)) {
                int deletedRecords = stepExecution.getExecutionContext().getInt("deletedRecords", 0);
                log.info("데이터 정리: {}건 삭제", deletedRecords);
            }
        });

        log.info("========================================");
    }
}