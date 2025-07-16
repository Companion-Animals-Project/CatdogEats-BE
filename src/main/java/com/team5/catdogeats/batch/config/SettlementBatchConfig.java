package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.batch.dto.SettlementBatchItem;
import com.team5.catdogeats.batch.mapper.SettlementCreateRowMapper;
import com.team5.catdogeats.batch.mapper.SettlementCompleteRowMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.math.BigDecimal;

/**
 * 정산 배치 설정
 */
@Slf4j
@Configuration
public class SettlementBatchConfig {

    private final JobRepository jobRepository;
    private final SettlementBatchProperties batchProperties;
    private final DataSource dataSource;
    private final PlatformTransactionManager batchTransactionManager;


    public SettlementBatchConfig(
            JobRepository jobRepository,
            SettlementBatchProperties batchProperties,
            DataSource dataSource,
            @Qualifier("batchTransactionManager") PlatformTransactionManager batchTransactionManager) {
        this.jobRepository = jobRepository;
        this.batchProperties = batchProperties;
        this.dataSource = dataSource;
        this.batchTransactionManager = batchTransactionManager;
    }

    /**
     * 정산 데이터 생성 배치 작업 (매일 오전 2시)
     */
    @Bean(name = "settlementChunkDailyJob")
    public Job settlementChunkDailyJob() {
        return new JobBuilder("settlementChunkDailyJob", jobRepository)
                .start(createSettlementsCursorStep())
                .build();
    }

    /**
     * 정산 완료 처리 배치 작업 (매월 1일 오전 4시)
     */
    @Bean(name = "settlementChunkMonthlyJob")
    public Job settlementChunkMonthlyJob() {
        return new JobBuilder("settlementChunkMonthlyJob", jobRepository)
                .start(completeSettlementsCursorStep())
                .build();
    }

    /**
     * 정산 데이터 생성 Step
     */
    @Bean
    public Step createSettlementsCursorStep() {
        return new StepBuilder("createSettlementsCursorStep", jobRepository)
                .<SettlementBatchItem, SettlementBatchItem>chunk(batchProperties.getChunkSize(), batchTransactionManager)
                .reader(settlementCreateCursorReader())
                .processor(settlementCreateItemProcessor())
                .writer(settlementCreateBatchWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(batchProperties.getSkipLimit())
                .skipPolicy(customSkipPolicy())
                .retry(TransientDataAccessException.class)
                .retryLimit(batchProperties.getRetryLimit())
                .build();
    }

    /**
     * 정산 완료 처리 Step
     */
    @Bean
    public Step completeSettlementsCursorStep() {
        return new StepBuilder("completeSettlementsCursorStep", jobRepository)
                .<SettlementBatchItem, SettlementBatchItem>chunk(batchProperties.getChunkSize(), batchTransactionManager)
                .reader(settlementCompleteCursorReader())
                .processor(settlementCompleteItemProcessor())
                .writer(settlementCompleteBatchWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(batchProperties.getSkipLimit())
                .skipPolicy(customSkipPolicy())
                .retry(TransientDataAccessException.class)
                .retryLimit(batchProperties.getRetryLimit())
                .build();
    }

    /**
     * 정산 생성용 Cursor Reader
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<SettlementBatchItem> settlementCreateCursorReader() {
        return new JdbcCursorItemReaderBuilder<SettlementBatchItem>()
                .name("settlementCreateCursorReader")
                .dataSource(dataSource)
                .sql("""
                    SELECT 
                        s.seller_id,
                        oi.id as order_item_id,
                        o.order_number,
                        p.title as product_title,
                        (oi.price / oi.quantity) as item_price,
                        qty_series.qty_num as quantity_sequence
                    FROM shipments s
                    INNER JOIN orders o ON s.order_id = o.id 
                    INNER JOIN order_items oi ON o.id = oi.order_id
                    INNER JOIN products p ON oi.product_id = p.id
                    CROSS JOIN generate_series(1, oi.quantity) as qty_series(qty_num)
                    WHERE s.delivered_at IS NOT NULL
                      AND s.delivered_at <= CURRENT_DATE - INTERVAL '7 days'
                      AND s.delivered_at >= CURRENT_DATE - INTERVAL '90 days'
                      AND o.order_status = 'DELIVERED'
                      AND o.is_hidden = false
                      AND NOT EXISTS (
                          SELECT 1 FROM settlements st 
                          WHERE st.order_item_id = oi.id
                      )
                    ORDER BY s.delivered_at ASC, oi.id ASC, qty_series.qty_num ASC
                    """)
                .rowMapper(new SettlementCreateRowMapper())
                .build();
    }

    /**
     * 정산 완료용 Cursor Reader
     */
    @Bean
    @StepScope
    public JdbcCursorItemReader<SettlementBatchItem> settlementCompleteCursorReader() {
        return new JdbcCursorItemReaderBuilder<SettlementBatchItem>()
                .name("settlementCompleteCursorReader")
                .dataSource(dataSource)
                .sql("""
                    SELECT 
                        st.id as settlement_id,
                        st.seller_id,
                        o.order_number,
                        p.title as product_title,
                        st.settlement_amount
                    FROM settlements st
                    INNER JOIN order_items oi ON st.order_item_id = oi.id
                    INNER JOIN orders o ON oi.order_id = o.id
                    INNER JOIN products p ON oi.product_id = p.id
                    WHERE st.settlement_status = 'IN_PROGRESS'
                      AND st.created_at >= CURRENT_DATE - INTERVAL '60 days'
                    ORDER BY st.created_at ASC, st.id ASC
                    """)
                .rowMapper(new SettlementCompleteRowMapper())
                .build();
    }

    /**
     * 정산 생성용 Batch Writer
     */
    @Bean
    public JdbcBatchItemWriter<SettlementBatchItem> settlementCreateBatchWriter() {
        return new JdbcBatchItemWriterBuilder<SettlementBatchItem>()
                .dataSource(dataSource)
                .sql("""
                    INSERT INTO settlements (
                        id, seller_id, order_item_id, item_price,
                        commission_rate, commission_amount, settlement_amount,
                        settlement_status, created_at, updated_at
                    ) VALUES (
                        gen_random_uuid(), :sellerId, :orderItemId, :itemPrice,
                        :commissionRate, :commissionAmount, :settlementAmount,
                        'IN_PROGRESS', NOW(), NOW()
                    )
                    """)
                .beanMapped()
                .build();
    }

    /**
     * 정산 완료용 Batch Writer
     */
    @Bean
    public JdbcBatchItemWriter<SettlementBatchItem> settlementCompleteBatchWriter() {
        return new JdbcBatchItemWriterBuilder<SettlementBatchItem>()
                .dataSource(dataSource)
                .sql("""
                    UPDATE settlements 
                    SET settlement_status = 'COMPLETED',
                        settled_at = NOW(),
                        updated_at = NOW()
                    WHERE id = :settlementId
                      AND settlement_status = 'IN_PROGRESS'
                    """)
                .beanMapped()
                .build();
    }

    /**
     * 정산 생성용 Processor
     */
    @Bean
    public ItemProcessor<SettlementBatchItem, SettlementBatchItem> settlementCreateItemProcessor() {
        return item -> {

            BigDecimal commissionRate = batchProperties.getCommissionRate();
            SettlementBatchItem enrichedItem = SettlementBatchItem.forCreate(
                    item.getSellerId(),
                    item.getOrderItemId(),
                    item.getOrderNumber(),
                    item.getProductTitle(),
                    item.getItemPrice(),
                    commissionRate
            );

            // 유효성 검증
            return enrichedItem.isValidForCreate() ? enrichedItem : null;
        };
    }

    /**
     * 정산 완료용 Processor
     */
    @Bean
    public ItemProcessor<SettlementBatchItem, SettlementBatchItem> settlementCompleteItemProcessor() {
        return item -> {
            // 완료용 데이터 변환
            SettlementBatchItem completeItem = SettlementBatchItem.forComplete(
                    item.getSettlementId(),
                    item.getSellerId(),
                    item.getOrderNumber(),
                    item.getProductTitle(),
                    item.getSettlementAmount()
            );

            // 유효성 검증
            return completeItem.isValidForComplete() ? completeItem : null;
        };
    }

    /**
     * Skip 정책
     */
    @Bean
    public SkipPolicy customSkipPolicy() {
        return new SkipPolicy() {
            @Override
            public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {
                // Skip 한계치 체크
                if (skipCount >= batchProperties.getSkipLimit()) {
                    return false;
                }

                // 스킵 가능한 예외들
                return t instanceof IllegalArgumentException ||
                        t instanceof TransientDataAccessException ||
                        !(t instanceof DataAccessException);
            }
        };
    }
}