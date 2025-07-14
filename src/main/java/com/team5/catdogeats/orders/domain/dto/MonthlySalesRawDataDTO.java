package com.team5.catdogeats.orders.domain.dto;

public record MonthlySalesRawDataDTO(
        Integer month,
        Long totalAmount,
        Long orderCount,
        Long totalQuantity
) {}
