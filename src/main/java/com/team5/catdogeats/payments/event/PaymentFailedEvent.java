package com.team5.catdogeats.payments.event;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * 결제 실패 이벤트
 * PG 승인 실패 또는 만료 시 발행되어 주문 취소 및 보상 처리를 트리거한다
 */
public record PaymentFailedEvent(
        String orderId,
        Long orderNumber,
        String userId,
        String failureCode,
        String failureMessage,
        ZonedDateTime failedAt
) implements Serializable {

    /**
     * 결제 실패 이벤트 생성
     */
    public static PaymentFailedEvent of(
            String orderId,
            Long orderNumber,
            String userId,
            String failureCode,
            String failureMessage) {

        return new PaymentFailedEvent(
                orderId,
                orderNumber,
                userId,
                failureCode,
                failureMessage,
                ZonedDateTime.now()
        );
    }
}