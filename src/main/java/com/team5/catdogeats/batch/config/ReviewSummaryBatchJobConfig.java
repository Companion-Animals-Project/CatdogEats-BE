package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.batch.dto.ProductReviewBatchDto;
import com.team5.catdogeats.batch.dto.ReviewSummaryResult;
import com.team5.catdogeats.batch.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
public class ReviewSummaryBatchJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager batchTransactionManager;
    private final ProductBatchMapper productBatchMapper;
    private final ReviewBatchMapper reviewBatchMapper;
    private final ReviewSummaryLLMBatchMapper summaryMapper;
    private final ReviewSummaryItemProcessor itemProcessor;
    private final ReviewClassificationLLMCatHandmadeBatchMapper classificationCatHandmadeMapper;
    private final ReviewClassificationLLMCatFinishedBatchMapper classificationCatFinishedMapper;
    private final ReviewClassificationLLMDogHandmadeBatchMapper classificationDogHandmadeMapper;
    private final ReviewClassificationLLMDogFinishedBatchMapper classificationDogFinishedMapper;

    public ReviewSummaryBatchJobConfig(JobRepository jobRepository,
                                       @Qualifier("batchTransactionManager") PlatformTransactionManager batchTransactionManager,
                                       ProductBatchMapper productBatchMapper,
                                       ReviewBatchMapper reviewBatchMapper,
                                       ReviewSummaryLLMBatchMapper summaryMapper,
                                       ReviewClassificationLLMCatHandmadeBatchMapper classificationCatHandmadeMapper,
                                       ReviewClassificationLLMCatFinishedBatchMapper classificationCatFinishedMapper,
                                       ReviewClassificationLLMDogHandmadeBatchMapper classificationDogHandmadeMapper,
                                       ReviewClassificationLLMDogFinishedBatchMapper classificationDogFinishedMapper,
                                       ReviewSummaryItemProcessor itemProcessor) {
        this.jobRepository = jobRepository;
        this.batchTransactionManager = batchTransactionManager;
        this.productBatchMapper = productBatchMapper;
        this.reviewBatchMapper = reviewBatchMapper;
        this.summaryMapper = summaryMapper;
        this.classificationCatHandmadeMapper = classificationCatHandmadeMapper;
        this.classificationCatFinishedMapper = classificationCatFinishedMapper;
        this.classificationDogHandmadeMapper = classificationDogHandmadeMapper;
        this.classificationDogFinishedMapper = classificationDogFinishedMapper;
        this.itemProcessor = itemProcessor;
    }

    // ---- Reader 빈 등록 ----
    @Bean
    @StepScope
    public ProductReviewBatchDtoItemReader productReviewBatchDtoItemReaderCatHandmade(
            @Value("#{jobParameters['petCategory']}") String petCategory,
            @Value("#{jobParameters['productCategory']}") String productCategory
    ) {
        return new ProductReviewBatchDtoItemReader(
                productBatchMapper, reviewBatchMapper, summaryMapper, petCategory, productCategory
        );
    }

    @Bean
    @StepScope
    public ProductReviewBatchDtoItemReader productReviewBatchDtoItemReaderCatFinished(
            @Value("#{jobParameters['petCategory']}") String petCategory,
            @Value("#{jobParameters['productCategory']}") String productCategory
    ) {
        return new ProductReviewBatchDtoItemReader(
                productBatchMapper, reviewBatchMapper, summaryMapper, petCategory, productCategory
        );
    }

    @Bean
    @StepScope
    public ProductReviewBatchDtoItemReader productReviewBatchDtoItemReaderDogHandmade(
            @Value("#{jobParameters['petCategory']}") String petCategory,
            @Value("#{jobParameters['productCategory']}") String productCategory
    ) {
        return new ProductReviewBatchDtoItemReader(
                productBatchMapper, reviewBatchMapper, summaryMapper, petCategory, productCategory
        );
    }

    @Bean
    @StepScope
    public ProductReviewBatchDtoItemReader productReviewBatchDtoItemReaderDogFinished(
            @Value("#{jobParameters['petCategory']}") String petCategory,
            @Value("#{jobParameters['productCategory']}") String productCategory
    ) {
        return new ProductReviewBatchDtoItemReader(
                productBatchMapper, reviewBatchMapper, summaryMapper, petCategory, productCategory
        );
    }

    // ---- Writer 빈 등록 ----
    @Bean
    @StepScope
    public ReviewSummaryItemWriterCatHandmade reviewSummaryItemWriterCatHandmade() {
        return new ReviewSummaryItemWriterCatHandmade(summaryMapper, classificationCatHandmadeMapper);
    }

    @Bean
    @StepScope
    public ReviewSummaryItemWriterCatFinished reviewSummaryItemWriterCatFinished() {
        return new ReviewSummaryItemWriterCatFinished(summaryMapper, classificationCatFinishedMapper);
    }

    @Bean
    @StepScope
    public ReviewSummaryItemWriterDogHandmade reviewSummaryItemWriterDogHandmade() {
        return new ReviewSummaryItemWriterDogHandmade(summaryMapper, classificationDogHandmadeMapper);
    }

    @Bean
    @StepScope
    public ReviewSummaryItemWriterDogFinished reviewSummaryItemWriterDogFinished() {
        return new ReviewSummaryItemWriterDogFinished(summaryMapper, classificationDogFinishedMapper);
    }


    // ---- Step 정의 ----
    @Bean
    public Step reviewSummaryStepCatHandmade(
            ProductReviewBatchDtoItemReader productReviewBatchDtoItemReaderCatHandmade,
            ReviewSummaryItemWriterCatHandmade reviewSummaryItemWriterCatHandmade
    ) {
        return new StepBuilder("reviewSummaryStepCatHandmade", jobRepository)
                .<ProductReviewBatchDto, ReviewSummaryResult>chunk(1000, batchTransactionManager)
                .reader(productReviewBatchDtoItemReaderCatHandmade)
                .processor(itemProcessor)
                .writer(reviewSummaryItemWriterCatHandmade)
                .build();
    }

    @Bean
    public Step reviewSummaryStepCatFinished(
            ProductReviewBatchDtoItemReader productReviewBatchDtoItemReaderCatFinished,
            ReviewSummaryItemWriterCatFinished reviewSummaryItemWriterCatFinished
    ) {
        return new StepBuilder("reviewSummaryStepCatFinished", jobRepository)
                .<ProductReviewBatchDto, ReviewSummaryResult>chunk(1000, batchTransactionManager)
                .reader(productReviewBatchDtoItemReaderCatFinished)
                .processor(itemProcessor)
                .writer(reviewSummaryItemWriterCatFinished)
                .build();
    }

    @Bean
    public Step reviewSummaryStepDogHandmade(
            ProductReviewBatchDtoItemReader productReviewBatchDtoItemReaderDogHandmade,
            ReviewSummaryItemWriterDogHandmade reviewSummaryItemWriterDogHandmade
    ) {
        return new StepBuilder("reviewSummaryStepDogHandmade", jobRepository)
                .<ProductReviewBatchDto, ReviewSummaryResult>chunk(1000, batchTransactionManager)
                .reader(productReviewBatchDtoItemReaderDogHandmade)
                .processor(itemProcessor)
                .writer(reviewSummaryItemWriterDogHandmade)
                .build();
    }

    @Bean
    public Step reviewSummaryStepDogFinished(
            ProductReviewBatchDtoItemReader productReviewBatchDtoItemReaderDogFinished,
            ReviewSummaryItemWriterDogFinished reviewSummaryItemWriterDogFinished
    ) {
        return new StepBuilder("reviewSummaryStepDogFinished", jobRepository)
                .<ProductReviewBatchDto, ReviewSummaryResult>chunk(1000, batchTransactionManager)
                .reader(productReviewBatchDtoItemReaderDogFinished)
                .processor(itemProcessor)
                .writer(reviewSummaryItemWriterDogFinished)
                .build();
    }

    // ---- Job 정의 ----
    @Bean
    public Job reviewSummaryJobCatHandmade(Step reviewSummaryStepCatHandmade) {
        return new JobBuilder("reviewSummaryJobCatHandmade", jobRepository)
                .start(reviewSummaryStepCatHandmade)
                .build();
    }

    @Bean
    public Job reviewSummaryJobCatFinished(Step reviewSummaryStepCatFinished) {
        return new JobBuilder("reviewSummaryJobCatFinished", jobRepository)
                .start(reviewSummaryStepCatFinished)
                .build();
    }

    @Bean
    public Job reviewSummaryJobDogHandmade(Step reviewSummaryStepDogHandmade) {
        return new JobBuilder("reviewSummaryJobDogHandmade", jobRepository)
                .start(reviewSummaryStepDogHandmade)
                .build();
    }

    @Bean
    public Job reviewSummaryJobDogFinished(Step reviewSummaryStepDogFinished) {
        return new JobBuilder("reviewSummaryJobDogFinished", jobRepository)
                .start(reviewSummaryStepDogFinished)
                .build();
    }
}
