package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.batch.dto.ProductReviewBatchDto;
import com.team5.catdogeats.batch.dto.ReviewSummaryResult;
import com.team5.catdogeats.batch.mapper.*;
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

    @Bean
    public Step reviewSummaryStepCatHandmade() {
        ProductReviewBatchDtoItemReader reader = new ProductReviewBatchDtoItemReader(
                productBatchMapper, reviewBatchMapper, summaryMapper, "CAT", "HANDMADE"
        );
        ReviewSummaryItemWriterCatHandmade writer = new ReviewSummaryItemWriterCatHandmade(
                summaryMapper, classificationCatHandmadeMapper
        );
        return new StepBuilder("reviewSummaryStepCatHandmade", jobRepository)
                .<ProductReviewBatchDto, ReviewSummaryResult>chunk(1000, batchTransactionManager)
                .reader(reader)
                .processor(itemProcessor)
                .writer(writer)
                .build();
    }

    @Bean
    public Step reviewSummaryStepCatFinished() {
        ProductReviewBatchDtoItemReader reader = new ProductReviewBatchDtoItemReader(
                productBatchMapper, reviewBatchMapper, summaryMapper, "CAT", "FINISHED"
        );
        ReviewSummaryItemWriterCatFinished writer = new ReviewSummaryItemWriterCatFinished(
                summaryMapper, classificationCatFinishedMapper
        );
        return new StepBuilder("reviewSummaryStepCatFinished", jobRepository)
                .<ProductReviewBatchDto, ReviewSummaryResult>chunk(1000, batchTransactionManager)
                .reader(reader)
                .processor(itemProcessor)
                .writer(writer)
                .build();
    }

    @Bean
    public Step reviewSummaryStepDogHandmade() {
        ProductReviewBatchDtoItemReader reader = new ProductReviewBatchDtoItemReader(
                productBatchMapper, reviewBatchMapper, summaryMapper, "DOG", "HANDMADE"
        );
        ReviewSummaryItemWriterDogHandmade writer = new ReviewSummaryItemWriterDogHandmade(
                summaryMapper, classificationDogHandmadeMapper
        );
        return new StepBuilder("reviewSummaryStepDogHandmade", jobRepository)
                .<ProductReviewBatchDto, ReviewSummaryResult>chunk(1000, batchTransactionManager)
                .reader(reader)
                .processor(itemProcessor)
                .writer(writer)
                .build();
    }

    @Bean
    public Step reviewSummaryStepDogFinished() {
        ProductReviewBatchDtoItemReader reader = new ProductReviewBatchDtoItemReader(
                productBatchMapper, reviewBatchMapper, summaryMapper, "DOG", "FINISHED"
        );
        ReviewSummaryItemWriterDogFinished writer = new ReviewSummaryItemWriterDogFinished(
                summaryMapper, classificationDogFinishedMapper
        );
        return new StepBuilder("reviewSummaryStepDogFinished", jobRepository)
                .<ProductReviewBatchDto, ReviewSummaryResult>chunk(1000, batchTransactionManager)
                .reader(reader)
                .processor(itemProcessor)
                .writer(writer)
                .build();
    }

    // 각 Job에 해당 Step만 start로 등록
    @Bean
    public Job reviewSummaryJobCatHandmade() {
        return new JobBuilder("reviewSummaryJobCatHandmade", jobRepository)
                .start(reviewSummaryStepCatHandmade())
                .build();
    }
    @Bean
    public Job reviewSummaryJobCatFinished() {
        return new JobBuilder("reviewSummaryJobCatFinished", jobRepository)
                .start(reviewSummaryStepCatFinished())
                .build();
    }
    @Bean
    public Job reviewSummaryJobDogHandmade() {
        return new JobBuilder("reviewSummaryJobDogHandmade", jobRepository)
                .start(reviewSummaryStepDogHandmade())
                .build();
    }
    @Bean
    public Job reviewSummaryJobDogFinished() {
        return new JobBuilder("reviewSummaryJobDogFinished", jobRepository)
                .start(reviewSummaryStepDogFinished())
                .build();
    }

}
