package com.team5.catdogeats.reviews.repository;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.reviews.domain.mapping.ReviewsSummaryLLM;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewSummaryLLMRepository extends JpaRepository<ReviewsSummaryLLM, String> {
    void deleteAllByProduct(Products product);

    ReviewsSummaryLLM findTopByProductIdOrderByCreatedAtDesc(String productId);
}
