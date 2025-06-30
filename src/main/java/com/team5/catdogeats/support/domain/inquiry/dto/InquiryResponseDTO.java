package com.team5.catdogeats.support.domain.inquiry.dto;

import com.team5.catdogeats.support.domain.Inquires;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// 문의 범용 응답 DTO
// 생성, 수정, 답변 등록 시 공통으로 사용
public record InquiryResponseDTO(
        String inquiryId,
        String title,
        String content,
        String inquiryType,
        String inquiryStatus,
        String inquiryReceiveMethod,
        String urgentLevel,
        String createdAt,    // 포맷된 문자열로 제공
        String updatedAt,    // 포맷된 문자열로 제공
        String userName,
        String message       // 성공 메시지 등
) {


    // Inquires 엔티티에서 DTO 생성 (기본 변환)
    public static InquiryResponseDTO from(Inquires inquiry) {
        return from(inquiry, "작업이 성공적으로 완료되었습니다.");
    }

    // Inquires 엔티티에서 DTO 생성 (커스텀 메시지)
        public static InquiryResponseDTO from(Inquires inquiry, String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        return new InquiryResponseDTO(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getInquiryType().getDisplayName(),
                inquiry.getInquiryStatus().getDisplayName(),
                inquiry.getInquiryReceiveMethod().getDisplayName(),
                inquiry.getInquiryUrgentLevel().getDisplayName(),
                inquiry.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter),
                inquiry.getUpdatedAt().withZoneSameInstant(koreaZone).format(formatter),
                inquiry.getUsers().getName(),
                message
        );
    }

    // 문의 생성 완료 응답용
        public static InquiryResponseDTO created(Inquires inquiry) {
        return from(inquiry, "문의가 성공적으로 등록되었습니다.");
    }

    // 답변 등록 완료 응답용
        public static InquiryResponseDTO replied(Inquires reply) {
        return from(reply, "답변이 성공적으로 등록되었습니다.");
    }
}