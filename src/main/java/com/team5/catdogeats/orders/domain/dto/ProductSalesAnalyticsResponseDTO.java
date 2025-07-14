package com.team5.catdogeats.orders.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품별 매출 분석 응답")
public record ProductSalesAnalyticsResponseDTO(
        @Schema(description = "조회 타입", example = "yearly", allowableValues = {"yearly", "monthly"})
        String type,

        @Schema(description = "조회 년도", example = "2024")
        Integer year,

        @Schema(description = "조회 월 (monthly인 경우만)", example = "3")
        Integer month,

        @Schema(description = "해당 기간 총 매출액", example = "12500000")
        Long totalAmount,

        @Schema(description = "상품별 매출 데이터 (페이징)")
        ProductSalesPageResponseDTO products
) {
    /**
     * 빈 데이터로 응답 생성
     */
    public static ProductSalesAnalyticsResponseDTO empty(String type, Integer year, Integer month) {
        return new ProductSalesAnalyticsResponseDTO(
                type, year, month, 0L,
                ProductSalesPageResponseDTO.empty()
        );
    }
}