package com.team5.catdogeats.batch.sheduler;

import com.team5.catdogeats.reviews.service.ReviewSummaryBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewSummaryBatchScheduler {
    private final ReviewSummaryBatchService reviewSummaryBatchService;

    @Scheduled(cron = "0 0 0 * * *")
    public void runReviewSummaryBatch() {
        log.info("[배치요약] 전체 상품 리뷰요약 배치 시작");
        try {
            reviewSummaryBatchService.batchSummarizeAllProducts();
        } catch (Exception e) {
            log.error("[배치요약] 전체 배치 에러", e);
        }
        log.info("[배치요약] 전체 상품 리뷰요약 배치 종료");
    }
}
