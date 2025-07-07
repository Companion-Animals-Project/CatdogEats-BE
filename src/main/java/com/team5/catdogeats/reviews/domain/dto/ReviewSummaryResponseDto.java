package com.team5.catdogeats.reviews.domain.dto;

import java.util.List;

public record ReviewSummaryResponseDto(
        String productTitle,
        List<String> positiveReview,
        List<String> negativeReview
) {
}
