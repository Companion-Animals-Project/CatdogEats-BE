package com.team5.catdogeats.products.domain.dto;

// 특정 sellerId로 상품 조회시 response
public record MyProductResponseDto(
    String productId,
    String productName,
    Long reviewCount,
    Double averageStar,
    String imageId,
    String imageUrl
) {
}
