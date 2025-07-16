package com.team5.catdogeats.products.domain.dto;

import java.time.ZonedDateTime;

public record ProductDeliveredResponseDto(
        String productId,
        String productImage,
        String productName,
        ZonedDateTime deliveredAt
) {
}
