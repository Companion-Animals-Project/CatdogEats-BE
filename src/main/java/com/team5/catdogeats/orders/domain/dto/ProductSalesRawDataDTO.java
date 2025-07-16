package com.team5.catdogeats.orders.domain.dto;

public record ProductSalesRawDataDTO(
        String productId,
        String productName,
        Long totalAmount,
        Long quantity
) {}
