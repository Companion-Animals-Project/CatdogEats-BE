package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.batch.dto.ForecastBatchItem;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import com.team5.catdogeats.forecast.service.DailySalesAggregationService;
import com.team5.catdogeats.forecast.service.DemandForecastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * 수요예측 Spring Batch 설정
 */
@Slf4j
@Configuration
public class ForecastBatchJobConfig {

    private final JobRepository jobRepository;
    private final ForecastBatchProperties forecastBatchProperties;
    private final DemandForecastService demandForecastService;
    private final DailySalesAggregationService dailySalesAggregationService;
    private final DataSource dataSource;
    private final PlatformTransactionManager batchTransactionManager;


    public ForecastBatchJobConfig(
            JobRepository jobRepository,
            ForecastBatchProperties forecastBatchProperties,
            DemandForecastService demandForecastService,
            DailySalesAggregationService dailySalesAggregationService,
            DataSource dataSource,
            @Qualifier("batchTransactionManager") PlatformTransactionManager batchTransactionManager) {
        this.jobRepository = jobRepository;
        this.forecastBatchProperties = forecastBatchProperties;
        this.demandForecastService = demandForecastService;
        this.dailySalesAggregationService = dailySalesAggregationService;
        this.dataSource = dataSource;
        this.batchTransactionManager = batchTransactionManager;
    }

    // ================================
    // Job 정의
    // ================================

    @Bean(name = "forecastAggregationJob")
    public Job forecastAggregationJob() {
        return new JobBuilder("forecastAggregationJob", jobRepository)
                .start(aggregationStep())
                .build();
    }

    @Bean(name = "forecastPredictionJob")
    public Job forecastPredictionJob() {
        return new JobBuilder("forecastPredictionJob", jobRepository)
                .start(predictionStep())
                .build();
    }

    @Bean(name = "forecastCleanupJob")
    public Job forecastCleanupJob() {
        return new JobBuilder("forecastCleanupJob", jobRepository)
                .start(cleanupStep())
                .build();
    }

    // ================================
    // Step 정의
    // ================================

    @Bean
    public Step aggregationStep() {
        return new StepBuilder("aggregationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("일별 판매 집계 시작");

                    LocalDate yesterday = LocalDate.now().minusDays(1);
                    int aggregatedRecords = dailySalesAggregationService.aggregateDailySales(yesterday);

                    log.info("일별 판매 집계 완료 - 날짜: {}, 처리 건수: {}", yesterday, aggregatedRecords);

                    chunkContext.getStepContext().getStepExecution()
                            .getExecutionContext().putInt("aggregatedRecords", aggregatedRecords);

                    if (aggregatedRecords == 0) {
                        log.warn("집계된 판매 데이터가 없습니다. 어제({}) 주문이 없었거나 이미 집계되었을 수 있습니다.", yesterday);
                    }

                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, batchTransactionManager)
                .build();
    }

    @Bean
    public Step predictionStep() {
        return new StepBuilder("predictionStep", jobRepository)
                .<ForecastBatchItem, ForecastBatchItem>chunk(forecastBatchProperties.getChunkSize(), batchTransactionManager)
                .reader(forecastCursorItemReader())
                .processor(forecastBatchItemProcessor())
                .writer(dummyItemWriter()) // 더미 Writer 필수
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(forecastBatchProperties.getSkipLimit())
                .skipPolicy(forecastSkipPolicy())
                .listener(forecastSkipListener())
                .retry(TransientDataAccessException.class)
                .retry(DeadlockLoserDataAccessException.class)
                .retryLimit(forecastBatchProperties.getRetryLimit())
                .build();
    }

    @Bean
    public Step cleanupStep() {
        return new StepBuilder("cleanupStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    log.info("오래된 데이터 정리 시작");

                    LocalDate cutoffDate = LocalDate.now().minusDays(forecastBatchProperties.getCleanupOldDataDays());
                    int deletedCount = demandForecastService.cleanupOldForecasts(cutoffDate);

                    log.info("오래된 데이터 정리 완료 - 기준일: {}, 삭제 건수: {}", cutoffDate, deletedCount);

                    chunkContext.getStepContext().getStepExecution()
                            .getExecutionContext().putInt("deletedRecords", deletedCount);

                    return org.springframework.batch.repeat.RepeatStatus.FINISHED;
                }, batchTransactionManager)
                .build();
    }

    // ================================
    // ItemReader, ItemProcessor, ItemWriter
    // ================================

    /**
     * Cursor 기반 ItemReader
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<ForecastBatchItem> forecastCursorItemReader() {
        return new JdbcCursorItemReaderBuilder<ForecastBatchItem>()
                .name("forecastCursorItemReader")
                .dataSource(dataSource)
                .sql("""
                    SELECT 
                        s.user_id as seller_id,
                        s.vendor_name,
                        CASE WHEN s.deleted_at IS NULL THEN true ELSE false END as is_active,
                        DATE(s.created_at) as join_date,
                        NULL as last_order_date,
                        (
                            SELECT COUNT(*)
                            FROM products p
                            WHERE p.seller_id = s.user_id
                        ) as total_product_count,
                        (
                            SELECT COUNT(*)
                            FROM products p
                            WHERE p.seller_id = s.user_id
                            AND p.stock > 0
                        ) as active_product_count
                    FROM sellers s
                    INNER JOIN users u ON s.user_id = u.id
                    WHERE s.deleted_at IS NULL
                    AND (
                        SELECT COUNT(*)
                        FROM products p
                        WHERE p.seller_id = s.user_id
                        AND p.stock > 0
                    ) > 0
                    ORDER BY s.created_at ASC
                    """)
                .rowMapper(new ForecastBatchItemRowMapper())
                .build();
    }

    /**
     * 수요예측 ItemProcessor
     */
    @Bean
    public ItemProcessor<ForecastBatchItem, ForecastBatchItem> forecastBatchItemProcessor() {
        return item -> {
            long startTime = System.currentTimeMillis();

            // 1. 기본 유효성 검증
            if (!item.isValid()) {
                return item.withProcessingResult(
                        ForecastBatchItem.ProcessingResult.skipped("유효하지 않은 판매자 정보"));
            }

            // 2. 처리 대상 여부 확인
            if (!item.isEligibleForProcessing()) {
                return item.withProcessingResult(
                        ForecastBatchItem.ProcessingResult.skipped("비활성 판매자 또는 상품 없음"));
            }

            // 3. 실제 수요예측 실행 (예외는 SkipPolicy가 처리)
            int forecastedProductCount = demandForecastService.executeForecasting(item.sellerId());
            long processingTime = System.currentTimeMillis() - startTime;

            // 4. 예측 결과가 0개인 경우
            if (forecastedProductCount == 0) {
                return item.withProcessingResult(
                        ForecastBatchItem.ProcessingResult.skipped("예측 가능한 상품 없음 (데이터 부족)"));
            }

            // 5. 성공 결과 생성 (단순화)
            ForecastBatchItem.ProcessingResult result = ForecastBatchItem.ProcessingResult.success(
                    forecastedProductCount,
                    processingTime,
                    0.8, // 기본 신뢰도
                    0    // 재고 부족은 별도 모니터링에서 확인
            );

            return item.withProcessingResult(result);
        };
    }

    /**
     * 더미 ItemWriter - Spring Batch 청크 모드에서 필수
     * 실제 처리는 Processor에서 완료되므로 빈 구현
     */
    @Bean
    public ItemWriter<ForecastBatchItem> dummyItemWriter() {
        return chunk -> {
            // 아무것도 하지 않음 - Processor에서 이미 모든 처리 완료
            log.debug("청크 처리 완료 - 아이템 수: {}", chunk.size());
        };
    }

    /**
     * 단순한 ItemWriter (Spring Batch 표준 패턴)
     * 커스텀 통계 수집 제거 - Spring Batch 메트릭 활용
     */
    @Bean
    public ItemWriter<ForecastBatchItem> forecastBatchItemWriter() {
        return chunk -> {
            // 실제 비즈니스 로직은 Processor에서 이미 처리됨
            // Writer는 단순히 완료 확인만
            log.debug("배치 청크 처리 완료 - 아이템 수: {}", chunk.size());
        };
    }

    // ================================
    // SkipPolicy & SkipListener
    // ================================

    @Bean
    public SkipPolicy forecastSkipPolicy() {
        return (Throwable t, long skipCount) -> {
            // 데이터 접근 오류는 스킵
            if (t instanceof DataAccessException) {
                log.warn("데이터 접근 오류로 스킵 - skipCount: {}", skipCount);
                return true;
            }

            // 비즈니스 로직 검증 실패는 스킵
            if (t instanceof IllegalArgumentException) {
                log.warn("비즈니스 검증 실패로 스킵 - skipCount: {}", skipCount);
                return true;
            }

            // 일반적인 런타임 오류도 스킵 허용
            if (t instanceof RuntimeException) {
                log.warn("런타임 오류로 스킵 - skipCount: {}", skipCount);
                return true;
            }

            // 기타 예외는 스킵하지 않음
            log.error("스킵하지 않을 심각한 오류 - skipCount: {}", skipCount);
            return false;
        };
    }

    /**
     * SkipListener - 실패 상세 로깅만 담당
     */
    @Bean
    public SkipListener<ForecastBatchItem, ForecastBatchItem> forecastSkipListener() {
        return new SkipListener<ForecastBatchItem, ForecastBatchItem>() {
            @Override
            public void onSkipInProcess(ForecastBatchItem item, Throwable t) {
                log.error("수요예측 처리 실패 - sellerId: {}, vendorName: {}, 오류: {}",
                        item.sellerId(), item.vendorName(), t.getMessage());
            }

            @Override
            public void onSkipInRead(Throwable t) {
                log.error("판매자 조회 실패 - 오류: {}", t.getMessage());
            }

            @Override
            public void onSkipInWrite(ForecastBatchItem item, Throwable t) {
                log.error("결과 저장 실패 - sellerId: {}, 오류: {}",
                        item.sellerId(), t.getMessage());
            }
        };
    }

    // ================================
    // RowMapper
    // ================================

    /**
     * ForecastBatchItem용 RowMapper
     */
    private static class ForecastBatchItemRowMapper implements RowMapper<ForecastBatchItem> {
        @Override
        public ForecastBatchItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ForecastBatchItem(
                    rs.getString("seller_id"),
                    rs.getString("vendor_name"),
                    rs.getBoolean("is_active"),
                    rs.getDate("join_date") != null ? rs.getDate("join_date").toLocalDate() : null,
                    rs.getDate("last_order_date") != null ? rs.getDate("last_order_date").toLocalDate() : null,
                    rs.getInt("total_product_count"),
                    rs.getInt("active_product_count")
            );
        }
    }
}