package com.team5.catdogeats.orders.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "기간별 매출 분석 응답")
public record PeriodSalesAnalyticsResponseDTO(
        @Schema(description = "조회 년도", example = "2024")
        Integer year,

        @Schema(description = "해당 년도 총 매출액", example = "12500000")
        Long yearTotalAmount,

        @Schema(description = "해당 년도 총 주문건수", example = "245")
        Long yearTotalOrderCount,

        @Schema(description = "해당 년도 총 판매수량", example = "850")
        Long yearTotalQuantity,

        @Schema(description = "월별 매출 데이터 (1~12월)")
        List<MonthlySalesDataDTO> monthlyData
) {
    /**
     * 빈 데이터로 응답 생성 (데이터가 없는 경우)
     */
    public static PeriodSalesAnalyticsResponseDTO empty(Integer year) {
        return new PeriodSalesAnalyticsResponseDTO(
                year, 0L, 0L, 0L,
                List.of() // 빈 리스트
        );
    }
}
