package com.team5.catdogeats.orders.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "월별 매출 데이터")
public record MonthlySalesDataDTO(
        @Schema(description = "월 (1~12)", example = "3")
        Integer month,

        @Schema(description = "해당 월 총 매출액", example = "1200000")
        Long totalAmount,

        @Schema(description = "해당 월 주문건수", example = "45")
        Long orderCount,

        @Schema(description = "해당 월 판매수량", example = "150")
        Long totalQuantity
) {
    /**
     * 빈 월 데이터 생성 (매출이 없는 월)
     */
    public static MonthlySalesDataDTO empty(Integer month) {
        return new MonthlySalesDataDTO(month, 0L, 0L, 0L);
    }
}
