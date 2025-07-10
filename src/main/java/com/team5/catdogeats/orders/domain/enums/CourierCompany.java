package com.team5.catdogeats.orders.domain.enums;

import lombok.Getter;

/**
 * 택배사 열거형
 * 지원 택배사: 우체국택배, CJ대한통운, 한진택배, 로젠택배, 롯데택배
 */
@Getter
public enum CourierCompany {

    /**
     * 우체국택배
     */
    POST_OFFICE("우체국택배"),

    /**
     * CJ대한통운
     */
    CJ_LOGISTICS("CJ대한통운"),

    /**
     * 한진택배
     */
    HANJIN("한진택배"),

    /**
     * 로젠택배
     */
    LOGEN("로젠택배"),

    /**
     * 롯데택배
     */
    LOTTE("롯데택배");

    /**
     * 사용자에게 표시되는 택배사명
     */
    private final String displayName;

    CourierCompany(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 운송장 번호 기본 형식 검증
     * 택배사별 기본적인 운송장 번호 형식 확인
     * @param trackingNumber 검증할 운송장 번호
     * @return 유효한 형식인지 여부
     */
    public boolean isValidTrackingNumberFormat(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return false;
        }

        String normalized = trackingNumber.trim().toUpperCase();

        return switch (this) {
            case POST_OFFICE -> normalized.matches("^[0-9]{13}$"); // 13자리 숫자
            case CJ_LOGISTICS, HANJIN -> normalized.matches("^[0-9]{10,12}$"); // 10-12자리 숫자
            case LOGEN -> normalized.matches("^[0-9]{11,12}$"); // 11-12자리 숫자
            case LOTTE -> normalized.matches("^[0-9]{12,13}$"); // 12-13자리 숫자
        };
    }
}