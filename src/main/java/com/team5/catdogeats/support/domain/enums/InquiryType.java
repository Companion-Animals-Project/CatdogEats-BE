package com.team5.catdogeats.support.domain.enums;

// 1:1문의 등록 시, 1:1문의 유형입니다.

public enum InquiryType {
    PRODUCT("제품"), // 긴급도 -> MIDDLE
    ORDER("주문"), // 긴급도 -> MIDDLE (진행 관련)
    PAYMENT("결제"), // 긴급도 -> HIGH (돈 관련)
    DELIVERY("배송"), // 긴급도 -> MIDDLE (진행 관련)
    RETURN("환불/교환"), // 긴급도 -> HIGH (돈 관련)
    ACCOUNT("계정"), // 긴급도 -> MIDDLE (구매력이 높은 유저의 경우 HIGH로 분류하면 좋겠지만, 구매에 따른 고객 등급 분류는 없는 파트이므로,
    // 원래 계정 문의 파트는 LOW 레벨 긴급도로 다룰 예정이었으나 MIDDLE로 변경하게 되었습니다.)
    ETC("기타"); // 긴급도 -> LOW

    private final String displayName;

    InquiryType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
