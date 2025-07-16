package com.team5.catdogeats.support.domain.inquiry.dto.response;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.support.domain.Inquires;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

//문의 상세 조회용 응답 DTO (유저/관리자)
public record InquiryDetailResponseDTO(
        String inquiryId,
        String title,
        String content,
        String inquiryType,
        String inquiryStatus,
        String inquiryReceiveMethod,
        String urgentLevel,
        String createdAt,
        String updatedAt,

        // 문의자 정보
        String userName,

        // 주문 정보 (있는 경우)
        OrderInfo orderInfo,

        // 메시지 목록 (스레드 형태)
        List<InquiryMessageDTO> messages,

        // 첨부파일 목록 추가
        List<InquiryAttachmentDTO> attachments
) {
    private static final DateTimeFormatter ADMIN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter USER_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    public record OrderInfo(
            String orderId,
            String orderNumber,
            String orderDate
    ) {}

    // 관리자 상세 조회
    public static InquiryDetailResponseDTO forAdmin(Inquires inquiry,
                                                    List<InquiryMessageDTO> messages,
                                                    List<InquiryAttachmentDTO> attachments) {
        OrderInfo orderInfo = null;
        if (inquiry.getOrders() != null) {
            Orders order = inquiry.getOrders();
            orderInfo = new OrderInfo(
                    order.getId(),
                    order.getOrderNumber().toString(),
                    order.getCreatedAt().withZoneSameInstant(KOREA_ZONE).format(ADMIN_FORMATTER)
            );
        }

        return new InquiryDetailResponseDTO(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getInquiryType().getDisplayName(),
                inquiry.getInquiryStatus().getDisplayName(),
                inquiry.getInquiryReceiveMethod().getDisplayName(),
                inquiry.getInquiryUrgentLevel().getDisplayName(),
                inquiry.getCreatedAt().withZoneSameInstant(KOREA_ZONE).format(ADMIN_FORMATTER),
                inquiry.getUpdatedAt().withZoneSameInstant(KOREA_ZONE).format(ADMIN_FORMATTER),
                inquiry.getUsers().getName(),
                orderInfo,
                messages,
                attachments
        );
    }

    // 유저 상세 조회 (간소화)
    public static InquiryDetailResponseDTO forUser(Inquires inquiry,
                                                   List<InquiryMessageDTO> messages,
                                                   List<InquiryAttachmentDTO> attachments) {
        return new InquiryDetailResponseDTO(
                inquiry.getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                null, // 유저는 유형 불필요
                inquiry.getInquiryStatus().getDisplayName(),
                null, // 유저는 수신방법 불필요
                null, // 유저는 긴급도 불필요
                inquiry.getCreatedAt().withZoneSameInstant(KOREA_ZONE).format(USER_FORMATTER),
                null, // 유저는 수정일 불필요
                null, // 유저는 본인 이름 불필요
                null, // 유저는 주문정보 간소화
                messages,
                attachments
        );
    }
}