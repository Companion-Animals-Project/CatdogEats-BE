package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.batch.dto.ProductReviewBatchDto;
import com.team5.catdogeats.batch.dto.ReviewSummaryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class ReviewSummaryBatchJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager batchTransactionManager;
    private final ProductReviewBatchDtoItemReader itemReader;
    private final ReviewSummaryItemProcessor itemProcessor;
    private final ReviewSummaryItemWriter itemWriter;

    public ReviewSummaryBatchJobConfig(JobRepository jobRepository,
                                       @Qualifier("batchTransactionManager") PlatformTransactionManager batchTransactionManager,
                                       ProductReviewBatchDtoItemReader itemReader,
                                       ReviewSummaryItemProcessor itemProcessor,
                                       ReviewSummaryItemWriter itemWriter) {
        this.jobRepository = jobRepository;
        this.batchTransactionManager = batchTransactionManager;
        this.itemReader = itemReader;
        this.itemProcessor = itemProcessor;
        this.itemWriter = itemWriter;
    }

    @Bean
    public Step reviewSummaryStep() {
        return new StepBuilder("reviewSummaryStep", jobRepository)
                .<ProductReviewBatchDto, ReviewSummaryResult>chunk(1000, batchTransactionManager)
                .reader(itemReader)
                .processor(itemProcessor)
                .writer(itemWriter)
                .build();
    }

    @Bean
    public Job reviewSummaryJob() {
        return new JobBuilder("reviewSummaryJob", jobRepository)
                .start(reviewSummaryStep())
                .build();
    }

}
