package com.team5.catdogeats.reviews.service.impl;

import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.mapping.ReviewsSummaryLLM;
import com.team5.catdogeats.reviews.repository.ReviewClassificationLLMRepository;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.reviews.repository.ReviewSummaryLLMRepository;
import com.team5.catdogeats.reviews.service.ReviewSummaryBatchService;
import com.team5.catdogeats.reviews.service.ReviewSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSummaryBatchServiceImpl implements ReviewSummaryBatchService {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewSummaryLLMRepository reviewSummaryLLMRepository;
    private final ReviewSummaryService reviewSummaryService;
    private final ReviewClassificationLLMRepository reviewClassificationLLMRepository;

    @JpaTransactional
    @Override
    public void batchSummarizeAllProducts() {
        List<Products> allProducts = productRepository.findAll();

        for (Products product : allProducts) {
            try {
                ReviewsSummaryLLM latestSummary = reviewSummaryLLMRepository.findTopByProductOrderByCreatedAtDesc(product);
                List<Reviews> latestReviews = reviewRepository.findTop30ByProductOrderByCreatedAtDesc(product);

                if (latestReviews.isEmpty()) {
                    // 리뷰 삭제되서 없는데 기존 요약 있으면 삭제
                    if (latestSummary != null) {
                        reviewSummaryLLMRepository.delete(latestSummary);
                        reviewClassificationLLMRepository.deleteAllByProduct(product);
                        log.info("[배치요약] 상품 {}: 리뷰 없음 → 기존 요약 삭제", product.getTitle());
                    }
                    continue;
                }

                // 기존 요약이 있으면, 리뷰의 updatedAt과 비교
                if (latestSummary != null) {
                    // 리뷰 수정 및 추가 감지
                    ZonedDateTime summaryCreatedAt = latestSummary.getCreatedAt();
                    boolean hasNewReview = latestReviews.stream()
                            .anyMatch(r -> r.getUpdatedAt().isAfter(summaryCreatedAt));

                    // 리뷰 개수 변화 감지 (삭제 감지)
                    int lastSummaryReviewCount = latestSummary.getReviewCount();
                    boolean countChanged = lastSummaryReviewCount != latestReviews.size();

                    if (!hasNewReview && !countChanged) {
                        log.info("[배치요약] 상품 {}: 기존 요약 최신, skip", product.getTitle());
                        continue;
                    }
                    // 기존 요약 먼저 삭제 (리뷰 요약 이후 최신 리뷰가 하나라도 생기면)
                    reviewSummaryLLMRepository.delete(latestSummary);
                    // 기존 요약 분류 결과들도 삭제
                    reviewClassificationLLMRepository.deleteAllByProduct(product);
                }

                // **기존 단일 상품 요약 메서드 호출** (forceRefresh=true)
                // 내부에서 요약 저장까지 처리
                reviewSummaryService.summarizeReviewsByProductNumber(product.getProductNumber(), true);

                log.info("[배치요약] 상품 {}: 요약 새로 저장", product.getTitle());

            } catch (Exception e) {
                log.error("[배치요약] 상품 {} 요약 중 에러: {}", product.getTitle(), e.getMessage(), e);
            }
        }
    }
}
