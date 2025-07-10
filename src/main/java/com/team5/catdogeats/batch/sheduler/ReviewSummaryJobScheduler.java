package com.team5.catdogeats.batch.sheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewSummaryJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job reviewSummaryJobCatHandmade;
    private final Job reviewSummaryJobCatFinished;
    private final Job reviewSummaryJobDogHandmade;
    private final Job reviewSummaryJobDogFinished;

    @Scheduled(cron = "0 0 0 * * *")
    public void runReviewSummaryBatchCatHandmade() {
        runJob("CAT+HANDMADE", reviewSummaryJobCatHandmade);
    }
    @Scheduled(cron = "0 0 0 * * *")
    public void runReviewSummaryBatchCatFinished() {
        runJob("CAT+FINISHED", reviewSummaryJobCatFinished);
    }
    @Scheduled(cron = "0 0 0 * * *")
    public void runReviewSummaryBatchDogHandmade() {
        runJob("DOG+HANDMADE", reviewSummaryJobDogHandmade);
    }
    @Scheduled(cron = "0 0 0 * * *")
    public void runReviewSummaryBatchDogFinished() {
        runJob("DOG+FINISHED", reviewSummaryJobDogFinished);
    }

    private void runJob(String name, Job job) {
        log.info("[배치요약] {} 리뷰요약 배치 시작", name);
        try {
            jobLauncher.run(
                    job,
                    new JobParametersBuilder().addLong("time", System.currentTimeMillis()).toJobParameters()
            );
        } catch (Exception e) {
            log.error("[배치요약] {} 배치 에러", name, e);
        }
        log.info("[배치요약] {} 리뷰요약 배치 종료", name);
    }
}
