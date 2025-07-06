package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.batch.reader.SettlementCompleteItemReader;
import com.team5.catdogeats.batch.reader.SettlementCreateItemReader;
import com.team5.catdogeats.batch.reader.SettlementUpdateItemReader;
import com.team5.catdogeats.batch.writer.SettlementCompleteItemWriter;
import com.team5.catdogeats.batch.writer.SettlementCreateItemWriter;
import com.team5.catdogeats.batch.writer.SettlementUpdateItemWriter;
import com.team5.catdogeats.batch.dto.SettlementBatchItem;
import com.team5.catdogeats.batch.mapper.SettlementChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * 정산 청크 기반 배치 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SettlementChunkBatchConfig {

    private final JobRepository jobRepository;
    private final SettlementChunkMapper settlementChunkMapper;
    private final SettlementBatchProperties batchProperties;

    @Qualifier("batchTransactionManager")
    private final PlatformTransactionManager batchTransactionManager;

    /**
     * 정산 데이터 생성/갱신 청크 배치 작업 (매일 오전 2시)
     */
    @Bean(name = "settlementChunkDailyJob")
    public Job settlementChunkDailyJob() {
        return new JobBuilder("settlementChunkDailyJob", jobRepository)
                .start(createSettlementsChunkStep())
                .next(updateSettlementsChunkStep())
                .build();
    }

    /**
     * 정산 완료 처리 청크 배치 작업 (매월 1일 오전 4시)
     */
    @Bean(name = "settlementChunkMonthlyJob")
    public Job settlementChunkMonthlyJob() {
        return new JobBuilder("settlementChunkMonthlyJob", jobRepository)
                .start(completeSettlementsChunkStep())
                .build();
    }

    /**
     * 정산 데이터 생성 청크 스텝
     */
    @Bean
    public Step createSettlementsChunkStep() {
        return new StepBuilder("createSettlementsChunkStep", jobRepository)
                .<SettlementBatchItem, SettlementBatchItem>chunk(batchProperties.getChunkSize(), batchTransactionManager)
                .reader(settlementCreateItemReader())
                .processor(settlementCreateItemProcessor())
                .writer(settlementCreateItemWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(batchProperties.getSkipLimit())
                .skipPolicy(customSkipPolicy())
                .retry(TransientDataAccessException.class)
                .retry(DeadlockLoserDataAccessException.class)
                .retryLimit(batchProperties.getRetryLimit())
                .listener(createStepExecutionListener())
                .build();
    }

    /**
     * 정산 상태 갱신 청크 스텝 (PENDING -> IN_PROGRESS)
     */
    @Bean
    public Step updateSettlementsChunkStep() {
        return new StepBuilder("updateSettlementsChunkStep", jobRepository)
                .<SettlementBatchItem, SettlementBatchItem>chunk(batchProperties.getChunkSize(), batchTransactionManager)
                .reader(settlementUpdateItemReader())
                .processor(settlementUpdateItemProcessor())
                .writer(settlementUpdateItemWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(batchProperties.getSkipLimit())
                .skipPolicy(customSkipPolicy())
                .retry(TransientDataAccessException.class)
                .retry(DeadlockLoserDataAccessException.class)
                .retryLimit(batchProperties.getRetryLimit())
                .listener(updateStepExecutionListener())
                .build();
    }

    /**
     * 정산 완료 처리 청크 스텝 (IN_PROGRESS -> COMPLETED)
     */
    @Bean
    public Step completeSettlementsChunkStep() {
        return new StepBuilder("completeSettlementsChunkStep", jobRepository)
                .<SettlementBatchItem, SettlementBatchItem>chunk(batchProperties.getChunkSize(), batchTransactionManager)
                .reader(settlementCompleteItemReader())
                .processor(settlementCompleteItemProcessor())
                .writer(settlementCompleteItemWriter())
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(batchProperties.getSkipLimit())
                .skipPolicy(customSkipPolicy())
                .retry(TransientDataAccessException.class)
                .retry(DeadlockLoserDataAccessException.class)
                .retryLimit(batchProperties.getRetryLimit())
                .listener(completeStepExecutionListener())
                .build();
    }

    /**
     * ItemReader 빈들
     */
    @Bean
    public SettlementCreateItemReader settlementCreateItemReader() {
        return new SettlementCreateItemReader(settlementChunkMapper,
                batchProperties.getCommissionRate(), batchProperties.getChunkSize());
    }

    @Bean
    public SettlementUpdateItemReader settlementUpdateItemReader() {
        return new SettlementUpdateItemReader(settlementChunkMapper, batchProperties.getChunkSize());
    }

    @Bean
    public SettlementCompleteItemReader settlementCompleteItemReader() {
        return new SettlementCompleteItemReader(settlementChunkMapper, batchProperties.getChunkSize());
    }

    /**
     * ItemWriter 빈들
     */
    @Bean
    public SettlementCreateItemWriter settlementCreateItemWriter() {
        return new SettlementCreateItemWriter(settlementChunkMapper);
    }

    @Bean
    public SettlementUpdateItemWriter settlementUpdateItemWriter() {
        return new SettlementUpdateItemWriter(settlementChunkMapper);
    }

    @Bean
    public SettlementCompleteItemWriter settlementCompleteItemWriter() {
        return new SettlementCompleteItemWriter(settlementChunkMapper);
    }

    /**
     * ItemProcessor 빈들 (검증 및 로깅용)
     */
    @Bean
    public ItemProcessor<SettlementBatchItem, SettlementBatchItem> settlementCreateItemProcessor() {
        return item -> {
            log.debug("정산 생성 처리 - orderNumber: {}, itemPrice: {}",
                    item.getOrderNumber(), item.getItemPrice());

            // 유효성 검증
            if (!item.isValidForCreate()) {
                log.warn("정산 생성 데이터 검증 실패 - 스킵 처리: {}", item);
                return null; // null 반환하면 해당 아이템은 Writer로 전달되지 않음
            }

            return item;
        };
    }

    @Bean
    public ItemProcessor<SettlementBatchItem, SettlementBatchItem> settlementUpdateItemProcessor() {
        return item -> {
            log.debug("정산 상태 갱신 처리 - settlementId: {}, orderNumber: {}",
                    item.getSettlementId(), item.getOrderNumber());

            // 유효성 검증
            if (!item.isValidForUpdate()) {
                log.warn("정산 상태 갱신 데이터 검증 실패 - 스킵 처리: {}", item);
                return null;
            }

            return item;
        };
    }

    @Bean
    public ItemProcessor<SettlementBatchItem, SettlementBatchItem> settlementCompleteItemProcessor() {
        return item -> {
            log.debug("정산 완료 처리 - settlementId: {}, settlementAmount: {}",
                    item.getSettlementId(), item.getSettlementAmount());

            // 유효성 검증
            if (!item.isValidForComplete()) {
                log.warn("정산 완료 데이터 검증 실패 - 스킵 처리: {}", item);
                return null;
            }

            return item;
        };
    }

    /**
     * 커스텀 Skip 정책
     * 어떤 예외를 스킵할지 결정
     */
    @Bean
    public SkipPolicy customSkipPolicy() {
        return new SkipPolicy() {
            @Override
            public boolean shouldSkip(Throwable t, long skipCount) throws SkipLimitExceededException {

                // Skip 한계치 초과 시 Job 실패
                if (skipCount >= batchProperties.getSkipLimit()) {
                    log.error("Skip 한계치 초과 - skipCount: {}, limit: {}", skipCount, batchProperties.getSkipLimit());
                    return false;
                }

                // 데이터 검증 실패는 스킵
                if (t instanceof IllegalArgumentException) {
                    log.warn("데이터 검증 실패로 스킵 - skipCount: {}, error: {}", skipCount, t.getMessage());
                    return true;
                }

                // 일시적 DB 오류는 재시도 후 스킵
                if (t instanceof TransientDataAccessException) {
                    log.warn("일시적 DB 오류로 스킵 - skipCount: {}, error: {}", skipCount, t.getMessage());
                    return true;
                }

                // 데드락은 재시도 후 스킵
                if (t instanceof DeadlockLoserDataAccessException) {
                    log.warn("데드락 발생으로 스킵 - skipCount: {}, error: {}", skipCount, t.getMessage());
                    return true;
                }

                // 일반적인 DB 오류는 스킵하지 않고 Job 실패
                if (t instanceof DataAccessException) {
                    log.error("심각한 DB 오류 발생 - Job 중단. error: {}", t.getMessage());
                    return false;
                }

                // 기타 예외는 스킵
                log.warn("예상치 못한 오류로 스킵 - skipCount: {}, error: {}", skipCount, t.getMessage());
                return true;
            }
        };
    }

    /**
     * Step 실행 리스너들 (진행 상황 모니터링)
     */
    @Bean
    public SettlementStepExecutionListener createStepExecutionListener() {
        return new SettlementStepExecutionListener("정산생성");
    }

    @Bean
    public SettlementStepExecutionListener updateStepExecutionListener() {
        return new SettlementStepExecutionListener("정산갱신");
    }

    @Bean
    public SettlementStepExecutionListener completeStepExecutionListener() {
        return new SettlementStepExecutionListener("정산완료");
    }
}