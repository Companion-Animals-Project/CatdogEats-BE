package com.team5.catdogeats.reviews.domain.dto;

import java.util.List;

public record ReviewSummaryItemDto(
        String summary,
        List<String> mainPoints,
        List<String> keywords
) {
}
