package com.team5.catdogeats.support.domain.inquiry.dto;


import com.team5.catdogeats.support.domain.Inquires;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// 사용자용 간소화된 목록 DTO
public record UserInquiryListResponseDTO(
        String inquiryId,
        String title,
        String contentPreview,    // 내용 일부 (20자 정도)
        String inquiryStatus,     // "답변 완료", "답변 대기"
        String createdAt
) {
    public static UserInquiryListResponseDTO from(Inquires inquiry) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        // 내용 미리보기 (20자로 제한)
        String contentPreview = inquiry.getContent().length() > 20
                ? inquiry.getContent().substring(0, 20) + "..."
                : inquiry.getContent();

        return new UserInquiryListResponseDTO(
                inquiry.getId(),
                inquiry.getTitle(),
                contentPreview,
                inquiry.getInquiryStatus().getDisplayName(),  // 한글 상태
                inquiry.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter)
        );
    }
}
