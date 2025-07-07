package com.team5.catdogeats.reviews.domain.dto;

public record ReviewClassificationResultDto(
        String review,
        String result, // 검증 결과
        String sentiment, // 분류 결과 "positive", "negative", ""
        String reason // 검증 결과 이유
) {
}
