package com.team5.catdogeats.products.domain.dto;

import java.util.List;

public record ProductDetailResponseDto(
        String title,
        String subTitle,
        String productInfo,
        String contents,
        boolean isDiscounted,
        Double discountRate,
        Long price,
        List<String> images,
        String vendorName,
        Double averageStar,
        int reviewCount
) {
}
