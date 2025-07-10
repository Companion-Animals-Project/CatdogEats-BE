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
}