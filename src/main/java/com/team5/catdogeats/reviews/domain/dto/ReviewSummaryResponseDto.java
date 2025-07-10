package com.team5.catdogeats.reviews.domain.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.util.List;

public record ReviewSummaryResponseDto(
        String productTitle,
        String positiveReview,
        List<String> positiveMainPoints,
        List<String> positiveKeywords,
        String negativeReview,
        List<String> negativeMainPoints,
        List<String> negativeKeywords
) {
}
