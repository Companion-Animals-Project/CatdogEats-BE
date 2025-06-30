package com.team5.catdogeats.support.domain.inquiry.dto;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

//문의 목록용 응답 DTO
public record InquiryListResponseDTO(
        String inquiryId,
        String inquiryNumber, // 관리자 페이지 - 문의 목록 넘버링 (#001, #002, ...)
        String title,
        String inquiryType, // 한글 유형 (PRODUCT -> 제품)
        String inquiryStatus, // 한글 상태 (PENDING -> 답변 대기)
        String inquiryUrgentLevel, // 한글 긴급도 (HIGH -> 높음)
        String createdAt,
        String userName,  // 문의자 이름
        boolean hasReply) {  // 답변 여부


    // Inquires 엔티티에서 DTO 생성
    public static InquiryListResponseDTO from(Inquires inquiry, int sequenceNumber) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        return new InquiryListResponseDTO(
                inquiry.getId(),
                String.format("#%03d", sequenceNumber),
                inquiry.getTitle(),
                inquiry.getInquiryType().getDisplayName(), // 한글 유형 (PRODUCT -> 제품)
                inquiry.getInquiryStatus().getDisplayName(), // 한글 상태 (PENDING -> 답변 대기)
                inquiry.getInquiryUrgentLevel().getDisplayName(), // 한글 긴급도 (HIGH -> 높음)
                inquiry.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter),
                inquiry.getUsers().getName(),
                inquiry.getInquiryStatus() == InquiryStatus.ANSWERED
        );
    }
}
