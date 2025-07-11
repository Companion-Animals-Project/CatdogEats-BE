package com.team5.catdogeats.orders.domain.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 매출 분석 조회 타입 열거형
 */
@Schema(description = "매출 분석 조회 타입")
public enum SalesAnalyticsType {

    @Schema(description = "연도별 조회")
    YEARLY("yearly"),

    @Schema(description = "월별 조회")
    MONTHLY("monthly");

    private final String value;

    SalesAnalyticsType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * 문자열을 Enum으로 변환
     */
    public static SalesAnalyticsType fromString(String value) {
        for (SalesAnalyticsType type : SalesAnalyticsType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("유효하지 않은 조회 타입입니다: " + value);
    }

    /**
     * 연도별 타입인지 확인
     */
    public boolean isYearly() {
        return this == YEARLY;
    }

    /**
     * 월별 타입인지 확인
     */
    public boolean isMonthly() {
        return this == MONTHLY;
    }
}