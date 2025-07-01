package com.team5.catdogeats.support.domain.inquiry.dto;

import com.team5.catdogeats.support.domain.Inquires;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// 문의 범용 응답 DTO
public record InquiryResponseDTO(
        String inquiryId,
        String title,
        String status,
        String createdAt,
        String updatedAt,
        String message
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    public static InquiryResponseDTO from(Inquires inquiry, String message) {
        return new InquiryResponseDTO(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getInquiryStatus().getDisplayName(),
                inquiry.getCreatedAt().withZoneSameInstant(KOREA_ZONE).format(FORMATTER),
                inquiry.getUpdatedAt().withZoneSameInstant(KOREA_ZONE).format(FORMATTER),
                message
        );
    }

    // 편의 메서드들
    public static InquiryResponseDTO created(Inquires inquiry) {
        return from(inquiry, "문의가 성공적으로 등록되었습니다.");
    }

    public static InquiryResponseDTO replied(Inquires inquiry) {
        return from(inquiry, "답변이 성공적으로 등록되었습니다.");
    }

    public static InquiryResponseDTO closed(Inquires inquiry) {
        return from(inquiry, "문의가 성공적으로 종료되었습니다.");
    }

    public static InquiryResponseDTO followupAdded(Inquires inquiry) {
        return from(inquiry, "답글이 성공적으로 등록되었습니다.");
    }
}