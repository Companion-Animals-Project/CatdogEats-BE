package com.team5.catdogeats.support.domain.enums;

public enum InquiryMessageType {
    QUESTION("최초 문의"), // 유저, 프론트에서는 안쓸 것 같습니다.
    ANSWER("최초 답변"), // 관리자, 프론트에서는 안쓸 것 같습니다.
    USER_FOLLOWUP("문의 내용"), // 답글에서도 역질문을 할 수 있기 때문에, QUESTION, ANSWER 네이밍을 피했습니다.
                                         // ex) 관리자 - "다른 이미지 파일을 추가로 올려주실 수 있나요?" / 유저 - "추가로 이미지 파일 올렸습니다."
    ADMIN_FOLLOWUP("답변 내용");

    private final String displayName;

    InquiryMessageType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
