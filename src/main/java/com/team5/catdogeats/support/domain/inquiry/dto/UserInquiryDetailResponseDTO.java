package com.team5.catdogeats.support.domain.inquiry.dto;

import com.team5.catdogeats.support.domain.Inquires;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 사용자용 간소화된 상세 DTO
public record UserInquiryDetailResponseDTO(
        String inquiryId,
        String title,
        String content,           // 전체 내용
        String inquiryStatus,     // "답변 완료", "답변 대기"
        String createdAt,
        List<InquiryReplyResponseDTO> replies, // 답변이 있으면 표시
        List<InquiryAttachmentDTO> attachedImages
) {
    public static UserInquiryDetailResponseDTO from(Inquires inquiry, List<InquiryReplyResponseDTO> replies,
                                                    List<InquiryAttachmentDTO> attachedImages) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        return new UserInquiryDetailResponseDTO(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getContent(),  // 전체 내용
                inquiry.getInquiryStatus().getDisplayName(),
                inquiry.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter),
                replies,
                attachedImages
        );
    }
}