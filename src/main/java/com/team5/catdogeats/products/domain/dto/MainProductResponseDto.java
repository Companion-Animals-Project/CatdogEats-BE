package com.team5.catdogeats.products.domain.dto;

import java.time.Instant;

public record MainProductResponseDto(
        String id,
        String productNumber,
        String imageUrl,
        String vendorName,
        String title,
        Double averageStar,
        int reviewCount,
        Long price,
        Boolean isDiscounted,
        Double discountRate,
        Long discountedPrice,
        Instant createdAt
) {
}
