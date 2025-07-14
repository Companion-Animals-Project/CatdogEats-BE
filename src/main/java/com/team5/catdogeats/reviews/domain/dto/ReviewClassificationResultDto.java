package com.team5.catdogeats.reviews.domain.dto;

import com.team5.catdogeats.reviews.domain.Reviews;
import lombok.Builder;

@Builder
public record ReviewClassificationResultDto(
        String review,
        String result, // 검증 결과
        String sentiment, // 분류 결과 "positive", "negative", ""
        String reason, // 검증 결과 이유
        Reviews reviewObj
) {
}
