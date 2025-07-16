package com.team5.catdogeats.forecast.domain.dto;

import java.time.LocalDate;

public record DailySalesDataDTO(
        String sellerId,
        String productId,
        LocalDate salesDate,
        Integer dailyQuantity,
        Long dailyRevenue,
        Integer orderCount
) {
}