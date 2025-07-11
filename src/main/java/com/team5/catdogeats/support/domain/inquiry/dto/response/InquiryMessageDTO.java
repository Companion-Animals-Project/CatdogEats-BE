package com.team5.catdogeats.support.domain.inquiry.dto.response;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryMessageType;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// 스레드 형태의 메시지를 표현하는 DTO
public record InquiryMessageDTO(
        String messageId,
        String content,
        String messageType,  // "문의 내용", "답변 내용"
        String authorType,   // "USER", "ADMIN"
        String createdAt
) {
    // Inquires 엔티티에서 DTO 생성
    public static InquiryMessageDTO from(Inquires inquiryMessage) {
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
                inquiryMessage.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter)
        );
    }
}
