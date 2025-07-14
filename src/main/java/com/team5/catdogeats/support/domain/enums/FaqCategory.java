package com.team5.catdogeats.support.domain.enums;

public enum FaqCategory {
    ALL("전체"), // 필터링용 추가
    PRODUCT("제품"),
    ORDER("주문/결제"),
    DELIVERY("배송"),
    RETURN("환불/교환"),
    ACCOUNT("계정"),
    ETC("기타");

    private final String displayName;

    FaqCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
