package com.team5.catdogeats.payments.event;

import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;

import java.time.LocalDateTime;
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
        Long orderNumber,

        // 사용자 정보
        String userId,
        String userProvider,
        String userProviderId,

        // 결제 정보
        String paymentId,
        String tossPaymentKey,
        Long finalAmount,

        // 주문 상품 정보 (OrderItems 생성용)
        List<OrderItemInfo> orderItems,

        // 배송지 정보 (Shipments 생성용)
        OrderCreateRequest.ShippingAddressRequest shippingAddress,

        // 쿠폰 할인 정보
        Long originalTotalPrice,
        Double couponDiscountRate,

        // 이벤트 발생 시각
        LocalDateTime eventOccurredAt
) {

    /**
     * 정적 팩토리 메서드
     */
    public static PaymentCompletedEvent of(
            String orderId,
            Long orderNumber,
            String userId,
            String userProvider,
            String userProviderId,
            String paymentId,
            String tossPaymentKey,
            Long finalAmount,
            List<OrderItemInfo> orderItems,
            OrderCreateRequest.ShippingAddressRequest shippingAddress,
            Long originalTotalPrice,
            Double couponDiscountRate) {

        return new PaymentCompletedEvent(
                orderId,
                orderNumber,
                userId,
                userProvider,
                userProviderId,
                paymentId,
                tossPaymentKey,
                finalAmount,
                orderItems,
                shippingAddress,
                originalTotalPrice,
                couponDiscountRate,
                LocalDateTime.now()
        );
    }

    /**
     * 쿠폰 할인 적용 여부 확인
     */
    public boolean isCouponApplied() {
        return couponDiscountRate != null && couponDiscountRate > 0;
    }

    /**
     * 주문 아이템 개수 조회
     */
    public int getOrderItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    /**
     * 첫 번째 상품명 조회 (알림용)
     */
    public String getFirstProductName() {
        return orderItems != null && !orderItems.isEmpty()
                ? orderItems.get(0).productName()
                : "상품";
    }
}