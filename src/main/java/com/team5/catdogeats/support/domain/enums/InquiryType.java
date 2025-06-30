package com.team5.catdogeats.support.domain.enums;

// 1:1문의 등록 시, 1:1문의 유형입니다.

public enum InquiryType {
    PRODUCT("제품"),
    ORDER("주문"),
    PAYMENT("결제"),
    DELIVERY("배송"),
    RETURN("환불/교환"),
    ACCOUNT("계정"),
    ETC("기타");

    private final String displayName;

    InquiryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
