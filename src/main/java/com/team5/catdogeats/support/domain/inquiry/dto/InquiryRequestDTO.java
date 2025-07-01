package com.team5.catdogeats.support.domain.inquiry.dto;

import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 답글/답변 등록
public record InquiryRequestDTO(
        @NotBlank(message = "내용은 필수입니다")
        @Size(min = 5, max = 2000, message = "내용은 5자 이상 2,000자 이하로 입력해주세요")
        String content,

        // 관리자 강제 종료
        @Size(max = 200, message = "종료 사유는 200자 이내로 입력해주세요")
        String reason,

        // 긴급도 변경
        InquiryUrgentLevel urgentLevel
) {
    // 답글/답변 등록 생성자
    public static InquiryRequestDTO forContent(String content) {
        return new InquiryRequestDTO(content, null, null);
    }

    // 관리자 강제 종료 생성자
    public static InquiryRequestDTO forClose(String reason) {
        return new InquiryRequestDTO(null, reason, null);
    }

    // 긴급도 변경 생성자
    public static InquiryRequestDTO forUrgentLevel(InquiryUrgentLevel urgentLevel) {
        return new InquiryRequestDTO(null, null, urgentLevel);
    }
}
