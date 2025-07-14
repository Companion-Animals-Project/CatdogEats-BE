package com.team5.catdogeats.batch.dto;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.mapping.ReviewsSummaryLLM;

import java.util.List;

public record ProductReviewBatchDto(
        Products product,
        List<Reviews> latestReviews,
        ReviewsSummaryLLM latestSummary
) {
}
