package com.team5.catdogeats.support.domain.inquiry.dto.response;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryMessageType;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 스레드 형태의 메시지를 표현하는 DTO
public record InquiryMessageDTO(
        String messageId,
        String content,
        String messageType,  // "문의 내용", "답변 내용"
        String authorType,   // "USER", "ADMIN"
        String createdAt,
        List<InquiryAttachmentDTO> attachments  // 각 메시지별 첨부파일 추가
) {
    // 기존 메서드 (첨부파일 없는 경우)
    public static InquiryMessageDTO from(Inquires inquiryMessage) {
        return from(inquiryMessage, List.of()); // 빈 리스트로 처리
    }

    // 첨부파일 포함 메서드 (새로 추가)
    public static InquiryMessageDTO from(Inquires inquiryMessage, List<InquiryAttachmentDTO> attachments) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        // 메시지 타입 기반으로 판단
        String authorType;
        if (inquiryMessage.getInquiryMessageType() == InquiryMessageType.ANSWER ||
                inquiryMessage.getInquiryMessageType() == InquiryMessageType.ADMIN_FOLLOWUP) {
            authorType = "ADMIN";
        } else {
            authorType = "USER";
        }

        return new InquiryMessageDTO(
                inquiryMessage.getId(),
                inquiryMessage.getContent(),
                inquiryMessage.getInquiryMessageType().getDisplayName(),
                authorType,
                inquiryMessage.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter),
                attachments  // 첨부파일 추가
        );
    }
}