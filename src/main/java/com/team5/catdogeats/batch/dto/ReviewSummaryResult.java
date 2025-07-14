package com.team5.catdogeats.batch.dto;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.reviews.domain.dto.ReviewClassificationResultDto;

import java.util.List;

public record ReviewSummaryResult(
        Products product,
        String positiveJson,
        String negativeJson,
        int reviewCount,
        List<ReviewClassificationResultDto> classificationResults,
        String positiveMainPoints,
        String negativeMainPoints,
        String positiveKeywords,
        String negativeKeywords
) {
}
