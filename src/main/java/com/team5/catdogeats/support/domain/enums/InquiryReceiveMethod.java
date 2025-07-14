package com.team5.catdogeats.support.domain.enums;

// 1:1문의 등록 시, 답변을 수신받는 방식입니다.

public enum InquiryReceiveMethod {
    WEB("문의 내역"), // 디폴트 수신 방법
    CALL("전화"),
    SMS("문자"),
    NONE("답변 불필요");

    private final String displayName;

    InquiryReceiveMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
