package com.team5.catdogeats.orders.domain.dto;

import com.team5.catdogeats.orders.domain.enums.SalesAnalyticsType;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 상품별 매출 분석 요청 파라미터 DTO
 */
@Schema(description = "상품별 매출 분석 요청 파라미터")
public record ProductSalesAnalyticsRequestDTO(
        @Schema(description = "조회 타입")
        SalesAnalyticsType type,

        @Schema(description = "조회 년도", example = "2024")
        Integer year,

        @Schema(description = "조회 월 (monthly인 경우 필수)", example = "3")
        Integer month
) {
    /**
     * 요청 파라미터 유효성 검증
     */
    public void validate() {
        if (type == null) {
            throw new IllegalArgumentException("조회 타입은 필수입니다");
        }

        if (year == null || year < 2020 || year > java.time.Year.now().getValue()) {
            throw new IllegalArgumentException("년도는 2020년부터 현재년도까지만 허용됩니다");
        }

        if (type.isMonthly()) {
            if (month == null || month < 1 || month > 12) {
                throw new IllegalArgumentException("월별 조회시 month는 1~12 사이의 값이어야 합니다");
            }
        }
    }

    /**
     * yearly 타입인지 확인
     */
    public boolean isYearly() {
        return type.isYearly();
    }

    /**
     * monthly 타입인지 확인
     */
    public boolean isMonthly() {
        return type.isMonthly();
    }
}
