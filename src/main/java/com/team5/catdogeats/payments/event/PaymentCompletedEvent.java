package com.team5.catdogeats.payments.event;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * 결제 완료 이벤트
 * PG 승인 성공 시 발행되어 주문상세, 배송정보 생성 및 알림을 트리거한다
 */
public record PaymentCompletedEvent(
        String orderId,
        Long orderNumber,
        String userId,
        Long amount,
        String paymentKey,
        ZonedDateTime completedAt
) implements Serializable {

    /**
     * 결제 완료 이벤트 생성
     */
    public static PaymentCompletedEvent of(
            String orderId,
            Long orderNumber,
            String userId,
            Long amount,
            String paymentKey) {

        return new PaymentCompletedEvent(
                orderId,
                orderNumber,
                userId,
                amount,
                paymentKey,
                ZonedDateTime.now()
        );
    }
}