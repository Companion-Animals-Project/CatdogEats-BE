package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 주간 매출 동향 DTO
 * 이번 주 동안의 일별 매출 데이터를 나타내는 DTO
 */
@Schema(description = "주간 매출 동향")
public record WeeklySalesDTO(
        @Schema(description = "매출 날짜 (YYYY-MM-DD)", example = "2025-01-15")
        String salesDate,

        @Schema(description = "해당 날짜 매출액 (원)", example = "450000")
        Long dailySales
) {}