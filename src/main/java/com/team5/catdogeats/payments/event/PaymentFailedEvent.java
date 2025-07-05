package com.team5.catdogeats.payments.event;

import java.time.ZonedDateTime;

/**
 * 결제 실패 이벤트
 * 결제가 실패했을 때 발행되는 이벤트
 * 주문 취소 및 재고 예약 해제 등의 처리를 위해 사용
 */
public record PaymentFailedEvent(
        // 주문 ID
        String orderId,

        // 주문 번호
        String orderNumber,

        // 실패 사유
        String errorCode,
        String errorMessage,

        // 이벤트 발생 시각
        ZonedDateTime eventOccurredAt
) {

    /**
     * 정적 팩토리 메서드
     */
    public static PaymentFailedEvent of(String orderId, String orderNumber, String errorCode, String errorMessage) {
        return new PaymentFailedEvent(
                orderId,
                orderNumber,
                errorCode,
                errorMessage,
                ZonedDateTime.now()
        );
    }
}