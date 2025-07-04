package com.team5.catdogeats.orders.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * 기간별 정산 조회 요청 DTO
 */
public record SettlementPeriodRequestDto(
        @NotNull(message = "시작일은 필수입니다")
        LocalDate startDate,    // 조회 시작일

        @NotNull(message = "종료일은 필수입니다")
        LocalDate endDate       // 조회 종료일
) {
}
