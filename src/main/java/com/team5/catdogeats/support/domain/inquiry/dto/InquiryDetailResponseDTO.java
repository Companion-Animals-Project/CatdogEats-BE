package com.team5.catdogeats.support.domain.inquiry.dto;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.support.domain.Inquires;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

//문의 상세 조회용 응답 DTO
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

        // 답변들
        List<InquiryReplyResponseDTO> replies,

        List<InquiryAttachmentDTO> attachedImages
) {

    // 주문 정보 DTO
    public record OrderInfo(
            String orderId,
            String orderNumber,
            String orderDate) {
    }

    // Inquires 엔티티에서 DTO 생성
    public static InquiryDetailResponseDTO from(Inquires inquiry, List<InquiryReplyResponseDTO> replies, List<InquiryAttachmentDTO> attachedImages) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        // 주문 정보 변환
        OrderInfo orderInfo = null;
        if (inquiry.getOrders() != null) {
            Orders order = inquiry.getOrders();
            orderInfo = new OrderInfo(
                    order.getId(),
                    order.getOrderNumber().toString(),
                    order.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter)
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
                inquiry.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter),
                inquiry.getUpdatedAt().withZoneSameInstant(koreaZone).format(formatter),
                inquiry.getUsers().getName(),
                orderInfo,
                replies,
                attachedImages
        );
    }
}
