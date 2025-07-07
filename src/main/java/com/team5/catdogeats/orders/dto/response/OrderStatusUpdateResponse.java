package com.team5.catdogeats.orders.dto.response;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * 주문 상태 변경 응답 DTO
 * API: POST /v1/sellers/orders/status
 */
@Builder
public record OrderStatusUpdateResponse(
        String orderNumber,         // 주문 번호
        OrderStatus previousStatus, // 이전 주문 상태
        OrderStatus currentStatus,  // 현재 주문 상태
        String reason,              // 상태 변경 사유
        ZonedDateTime updatedAt,    // 상태 변경 시각
        String message,             // 처리 결과 메시지

        // ===== 컴파일 오류 해결을 위한 필드들 =====
        Boolean isDelayed,          // 배송 지연 여부
        String delayReason,         // 배송 지연 사유
        ZonedDateTime expectedDeliveryDate // 예상 배송일
) {

    /**
     * 성공 응답 생성 - 기본
     */
    public static OrderStatusUpdateResponse success(
            String orderNumber,
            OrderStatus previousStatus,
            OrderStatus currentStatus,
            String message
    ) {
        return OrderStatusUpdateResponse.builder()
                .orderNumber(orderNumber)
                .previousStatus(previousStatus)
                .currentStatus(currentStatus)
                .updatedAt(ZonedDateTime.now())
                .message(message)
                .isDelayed(false)
                .build();
    }

    /**
     * 배송 지연 포함 응답 생성
     */
    public static OrderStatusUpdateResponse withDelay(
            String orderNumber,
            OrderStatus previousStatus,
            OrderStatus currentStatus,
            String delayReason,
            ZonedDateTime expectedDeliveryDate
    ) {
        return OrderStatusUpdateResponse.builder()
                .orderNumber(orderNumber)
                .previousStatus(previousStatus)
                .currentStatus(currentStatus)
                .updatedAt(ZonedDateTime.now())
                .message("주문 상태가 변경되었습니다 (배송 지연)")
                .isDelayed(true)
                .delayReason(delayReason)
                .expectedDeliveryDate(expectedDeliveryDate)
                .build();
    }
}