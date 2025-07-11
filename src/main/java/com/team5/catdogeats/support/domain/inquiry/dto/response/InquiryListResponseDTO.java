package com.team5.catdogeats.support.domain.inquiry.dto.response;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

//문의 목록용 응답 DTO (유저/관리자)
public record InquiryListResponseDTO(
        String inquiryId,
        String inquiryNumber, // 관리자용 (#001, #002, ...)
        String title,
        String contentPreview, // 사용자용 미리보기
        String inquiryType,
        String inquiryStatus,
        String inquiryUrgentLevel,
        String createdAt,
        String userName,
        boolean hasReply
) {
    private static final DateTimeFormatter ADMIN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter USER_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    // 관리자 목록
    public static InquiryListResponseDTO forAdmin(Inquires inquiry, int sequenceNumber) {
        return new InquiryListResponseDTO(
                inquiry.getId(),
                String.format("#%03d", sequenceNumber),
                inquiry.getTitle(),
                null, // 관리자는 미리보기 불필요
                inquiry.getInquiryType().getDisplayName(),
                inquiry.getInquiryStatus().getDisplayName(),
                inquiry.getInquiryUrgentLevel().getDisplayName(),
                inquiry.getCreatedAt().withZoneSameInstant(KOREA_ZONE).format(ADMIN_FORMATTER),
                inquiry.getUsers().getName(),
                inquiry.getInquiryStatus() == InquiryStatus.ANSWERED
        );
    }

    // 유저 목록
    public static InquiryListResponseDTO forUser(Inquires inquiry) {
        String contentPreview = inquiry.getContent().length() > 20
                ? inquiry.getContent().substring(0, 20) + "..."
                : inquiry.getContent();

        return new InquiryListResponseDTO(
                inquiry.getId(),
                null, // 유저는 넘버링 불필요
                inquiry.getTitle(),
                contentPreview,
                inquiry.getInquiryType().getDisplayName(),
                inquiry.getInquiryStatus().getDisplayName(),
                null, // 유저는 긴급도 불필요
                inquiry.getCreatedAt().withZoneSameInstant(KOREA_ZONE).format(USER_FORMATTER),
                null, // 유저는 본인 이름 불필요
                inquiry.getInquiryStatus() == InquiryStatus.ANSWERED
        );
    }
}
