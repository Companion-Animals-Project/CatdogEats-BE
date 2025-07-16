package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 오늘 주문 통계 DTO
 * 판매자 대시보드에서 오늘의 주문 수와 매출을 보여주는 데이터
 */
@Schema(description = "오늘 주문 통계")
public record TodayStatsDTO(
        @Schema(description = "오늘 주문 수", example = "25")
        Long todayOrderCount,

        @Schema(description = "오늘 총 매출액 (원)", example = "1250000")
        Long todayTotalSales
) {
    /**
     * 기본값으로 TodayStatsDTO 생성 (데이터가 없는 경우)
     */
    public static TodayStatsDTO empty() {
        return new TodayStatsDTO(0L, 0L);
    }
}