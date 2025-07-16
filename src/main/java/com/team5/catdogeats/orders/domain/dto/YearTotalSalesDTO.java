package com.team5.catdogeats.orders.domain.dto;

/**
 * 연도별 총 매출 데이터 DTO
 * findYearTotalSalesBySellerAndYear 메서드 전용
 */
public record YearTotalSalesDTO(
        Long totalAmount,
        Long orderCount,
        Long totalQuantity
) {
    /**
     * 빈 데이터 생성 (매출이 없는 경우)
     */
    public static YearTotalSalesDTO empty() {
        return new YearTotalSalesDTO(0L, 0L, 0L);
    }
}