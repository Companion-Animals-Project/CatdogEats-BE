package com.team5.catdogeats.support.domain.enums;

// 1:1문의 관리자 페이지의 긴급도입니다.

public enum InquiryUrgentLevel {
    HIGH("높음"),
    MIDDLE("중간"),
    LOW("낮음");

    private final String displayName;

    InquiryUrgentLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
