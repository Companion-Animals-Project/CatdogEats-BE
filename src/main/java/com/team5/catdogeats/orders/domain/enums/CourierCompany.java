package com.team5.catdogeats.orders.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 택배사 열거형
 * 스마트택배 API와 연동하여 배송 추적을 지원하는 택배사 목록
 * 각 택배사는 고유한 코드와 표시명을 가집니다.
 */
@Getter
@RequiredArgsConstructor
public enum CourierCompany {

    /**
     * 우체국택배
     * 스마트택배 API 코드: 01
     */
    POST_OFFICE("01", "우체국택배"),

    /**
     * CJ대한통운
     * 스마트택배 API 코드: 04
     */
    CJ_LOGISTICS("04", "CJ대한통운"),

    /**
     * 한진택배
     * 스마트택배 API 코드: 05
     */
    HANJIN("05", "한진택배"),

    /**
     * 로젠택배
     * 스마트택배 API 코드: 06
     */
    LOGEN("06", "로젠택배"),

    /**
     * 롯데택배
     * 스마트택배 API 코드: 08
     */
    LOTTE("08", "롯데택배");

    /**
     * 스마트택배 API에서 사용하는 택배사 코드
     */
    private final String apiCode;

    /**
     * 사용자에게 표시되는 택배사명
     */
    private final String displayName;

    /**
     * 스마트택배 API 코드로 택배사 검색
     * @param apiCode 스마트택배 API 코드
     * @return 해당하는 택배사 (Optional)
     */
    public static CourierCompany findByApiCode(String apiCode) {
        if (apiCode == null || apiCode.trim().isEmpty()) {
            return null;
        }

        for (CourierCompany courier : values()) {
            if (courier.apiCode.equals(apiCode.trim())) {
                return courier;
            }
        }
        return null;
    }

    /**
     * 표시명으로 택배사 검색
     * @param displayName 택배사 표시명
     * @return 해당하는 택배사 (Optional)
     */
    public static CourierCompany findByDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return null;
        }

        for (CourierCompany courier : values()) {
            if (courier.displayName.equals(displayName.trim())) {
                return courier;
            }
        }
        return null;
    }

    /**
     * 스마트택배 API 호출에 사용할 수 있는 유효한 코드인지 검증
     * @param apiCode 검증할 API 코드
     * @return 유효한 코드 여부
     */
    public static boolean isValidApiCode(String apiCode) {
        return findByApiCode(apiCode) != null;
    }
}