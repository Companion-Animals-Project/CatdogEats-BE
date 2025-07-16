package com.team5.catdogeats.products.domain.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.util.List;

public record ProductDetailResponseDto(
        String title,
        String subTitle,
        String productInfo,
        String contents,
        boolean isDiscounted,
        Double discountRate,
        Long price,
        Long discountedPrice,
        @JsonRawValue
        List<String> images,
        String vendorName,
        Double averageStar,
        int reviewCount
) {
}
