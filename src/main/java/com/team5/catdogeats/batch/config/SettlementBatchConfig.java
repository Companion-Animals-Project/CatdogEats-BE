package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.orders.service.SettlementBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SettlementBatchConfig {

    private final JobRepository jobRepository;
    private final SettlementBatchService settlementBatchService;

    @Qualifier("batchTransactionManager")
    private final PlatformTransactionManager batchTransactionManager;

    /**
     * 정산 데이터 생성/갱신 배치 작업 (매일 오전 2시)
     */
    @Bean
    public Job settlementDailyJob() {
        return new JobBuilder("settlementDailyJob", jobRepository)
                .start(createSettlementsStep())
                .next(updateSettlementsToInProgressStep())
                .build();
    }

    /**
     * 정산 완료 처리 배치 작업 (매월 1일 오전 4시)
     */
    @Bean
    public Job settlementMonthlyJob() {
        return new JobBuilder("settlementMonthlyJob", jobRepository)
                .start(completeSettlementsStep())
                .build();
    }

    /**
     * 정산 데이터 생성 스텝
     */
    @Bean
    public Step createSettlementsStep() {
        return new StepBuilder("createSettlementsStep", jobRepository)
                .tasklet(createSettlementsTasklet(), batchTransactionManager)
                .build();
    }

    /**
     * 정산 상태 갱신 스텝 (PENDING -> IN_PROGRESS)
     */
    @Bean
    public Step updateSettlementsToInProgressStep() {
        return new StepBuilder("updateSettlementsToInProgressStep", jobRepository)
                .tasklet(updateSettlementsToInProgressTasklet(), batchTransactionManager)
                .build();
    }

    /**
     * 정산 완료 처리 스텝 (IN_PROGRESS -> COMPLETED)
     */
    @Bean
    public Step completeSettlementsStep() {
        return new StepBuilder("completeSettlementsStep", jobRepository)
                .tasklet(completeSettlementsTasklet(), batchTransactionManager)
                .build();
    }

    /**
     * 정산 데이터 생성 태스클릿
     */
    @Bean
    public Tasklet createSettlementsTasklet() {
        return (contribution, chunkContext) -> {
            try {
                log.info("=== 정산 데이터 생성 태스클릿 시작 ===");

                // 처리 전 대상 건수 조회
                int targetCount = settlementBatchService.getUnsettledItemsCount();
                log.info("정산 데이터 생성 대상 건수: {}", targetCount);

                if (targetCount > 0) {
                    settlementBatchService.createSettlements();
                } else {
                    log.info("정산 데이터 생성 대상이 없습니다.");
                }

                log.info("=== 정산 데이터 생성 태스클릿 완료 ===");
                return RepeatStatus.FINISHED;
            } catch (Exception e) {
                log.error("정산 데이터 생성 태스클릿 실패", e);
                throw e;
            }
        };
    }

    /**
     * 정산 상태 갱신 태스클릿 (PENDING -> IN_PROGRESS)
     */
    @Bean
    public Tasklet updateSettlementsToInProgressTasklet() {
        return (contribution, chunkContext) -> {
            try {
                log.info("=== 정산 상태 갱신 태스클릿 시작 ===");

                // 처리 전 대상 건수 조회
                int targetCount = settlementBatchService.getPendingSettlementsReadyForProgressCount();
                log.info("정산 상태 갱신 대상 건수 (PENDING -> IN_PROGRESS): {}", targetCount);

                if (targetCount > 0) {
                    settlementBatchService.updateSettlementsToInProgress();
                } else {
                    log.info("정산 상태 갱신 대상이 없습니다.");
                }

                log.info("=== 정산 상태 갱신 태스클릿 완료 ===");
                return RepeatStatus.FINISHED;
            } catch (Exception e) {
                log.error("정산 상태 갱신 태스클릿 실패", e);
                throw e;
            }
        };
    }

    /**
     * 정산 완료 처리 태스클릿 (IN_PROGRESS -> COMPLETED)
     */
    @Bean
    public Tasklet completeSettlementsTasklet() {
        return (contribution, chunkContext) -> {
            try {
                log.info("=== 정산 완료 처리 태스클릿 시작 ===");

                // 처리 전 대상 건수 조회
                int targetCount = settlementBatchService.getInProgressSettlementsCount();
                log.info("정산 완료 처리 대상 건수 (IN_PROGRESS -> COMPLETED): {}", targetCount);

                if (targetCount > 0) {
                    settlementBatchService.completeSettlements();
                } else {
                    log.info("정산 완료 처리 대상이 없습니다.");
                }

                log.info("=== 정산 완료 처리 태스클릿 완료 ===");
                return RepeatStatus.FINISHED;
            } catch (Exception e) {
                log.error("정산 완료 처리 태스클릿 실패", e);
                throw e;
            }
        };
    }
}