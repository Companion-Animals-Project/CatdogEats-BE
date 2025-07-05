package com.team5.catdogeats.support.domain.enums;

public enum InquiryStatus {
    PENDING("답변 대기"),  // 최초 답변 대기 상태입니다.
    ANSWERED("답변 완료"), // 최초 답변 완료 상태입니다.
    FOLLOWUP("추가 문의"), // 유저가 추가 답글을 단 경우(관리자 답글 이후), 관리자가 추가 답글을 단 경우 2가지 모두 해당합니다.
    CLOSED("문의 종료"), // 유저/관리자 모두 대화가 종료된 상태입니다.
    FORCE_CLOSED("강제 종료"); // 관리자가 강제로 종료한 경우를 구분합니다. (블랙리스트 추적 혹은 통계 확장성)

    private final String displayName;

    InquiryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
