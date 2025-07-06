package com.team5.catdogeats.payments.event;

import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 결제 완료 이벤트
 * 결제가 성공적으로 완료되었을 때 발행되는 이벤트
 * 이 이벤트를 수신하여 실제 주문 상품과 배송 정보를 생성
 */
public record PaymentCompletedEvent(
        // 주문 ID
        String orderId,

        // 주문 번호
        String orderNumber,

        // 사용자 정보
        String buyerId,

        // 결제 정보
        String paymentId,
        String tossPaymentKey,

        // 주문 상품 정보 (OrderItems 생성용)
        List<OrderItemInfo> orderItems,

        // 배송지 정보 (Shipments 생성용)
        OrderCreateRequest.ShippingAddressRequest shippingAddress,

        // ===== 쿠폰 할인 정보 확장 =====
        Long originalTotalPrice,
        boolean couponApplied,
        Long discountAmount,
        Long discountedTotalPrice,

        // 이벤트 발생 시각
        ZonedDateTime eventOccurredAt
) {
    public static PaymentCompletedEvent of(
            String orderId,
            String orderNumber,
            String buyerId,
            String paymentId,
            String tossPaymentKey,
            List<OrderItemInfo> orderItems,
            OrderCreateRequest.ShippingAddressRequest shippingAddress,
            Long originalTotalPrice,
            boolean couponApplied,
            Long discountAmount,
            Long discountedTotalPrice) {

        return new PaymentCompletedEvent(
                orderId,
                orderNumber,
                buyerId,
                paymentId,
                tossPaymentKey,
                orderItems,
                shippingAddress,
                originalTotalPrice,
                couponApplied,
                discountAmount,
                discountedTotalPrice,
                ZonedDateTime.now());
    }

    // ===== 비즈니스 로직 메서드들 =====


    public int getOrderItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    public String getFirstProductName() {
        return orderItems != null && !orderItems.isEmpty()
                ? orderItems.get(0).productName()
                : "상품";
    }
}