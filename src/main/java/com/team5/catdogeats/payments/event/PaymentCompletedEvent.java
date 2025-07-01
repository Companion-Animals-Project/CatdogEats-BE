package com.team5.catdogeats.payments.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class PaymentCompletedEvent {

    private String paymentId;
    private String orderId;
    private String userId;
    private List<String> purchasedProductIds;
    private Long totalAmount;
    private ZonedDateTime completedAt;

    // 로깅용 toString
    @Override
    public String toString() {
        return String.format("PaymentCompletedEvent{paymentId='%s', orderId='%s', userId='%s', productCount=%d, amount=%d}",
                paymentId, orderId, userId,
                purchasedProductIds != null ? purchasedProductIds.size() : 0,
                totalAmount);
    }
}