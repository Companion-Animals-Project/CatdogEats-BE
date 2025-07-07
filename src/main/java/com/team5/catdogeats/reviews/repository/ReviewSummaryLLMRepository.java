package com.team5.catdogeats.reviews.repository;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.mapping.ReviewsSummaryLLM;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewSummaryLLMRepository extends JpaRepository<ReviewsSummaryLLM, String> {
    // 특정 상품의 요약 결과들 중, createdAt 기준 가장 최근(최신)의 한 건을 조회
    ReviewsSummaryLLM findTopByProductOrderByCreatedAtDesc(Products product);

    void deleteAllByProduct(Products product);
}
