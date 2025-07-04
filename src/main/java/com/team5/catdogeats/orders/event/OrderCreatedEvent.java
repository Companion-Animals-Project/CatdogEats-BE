package com.team5.catdogeats.orders.event;

import com.team5.catdogeats.orders.dto.common.OrderItemInfo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 생성 이벤트
 * 주문이 성공적으로 생성되었을 때 발행되는 이벤트입니다.
 * 담당 역할:
 * 1. 재고 예약 (orderItems 필요)
 * 2. 결제 정보 생성 (기본 주문 정보 필요)
 * 3. 사용자 알림 (주문 생성 완료 알림)
 * 최적화 포인트:
 * - shippingAddress 제거 (결제 완료 후에만 필요)
 * - 알림용 정보 최소화 (계산된 필드 활용)
 * - 재고 예약에 필요한 핵심 정보만 유지
 */
public record OrderCreatedEvent(
        // 핵심 주문 정보
        String orderId,
        Long orderNumber,

        // 사용자 정보 (결제 정보 생성용)
        String userId,
        String userProvider,
        String userProviderId,

        // 결제 금액 (결제 정보 생성 + 알림용)
        Long finalTotalPrice,

        // 재고 예약용 (필수)
        List<OrderItemInfo> orderItems,

        // 이벤트 발생 시각
        LocalDateTime eventOccurredAt
) {

    /**
     * 정적 팩토리 메서드 (간소화)
     */
    public static OrderCreatedEvent of(
            String orderId,
            Long orderNumber,
            String userId,
            String userProvider,
            String userProviderId,
            Long finalTotalPrice,
            List<OrderItemInfo> orderItems) {

        return new OrderCreatedEvent(
                orderId,
                orderNumber,
                userId,
                userProvider,
                userProviderId,
                finalTotalPrice,
                orderItems,
                LocalDateTime.now()
        );
    }

    /**
     * 편의 메서드들 (계산된 필드)
     */
    public int getOrderItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    public String getFirstProductName() {
        return orderItems != null && !orderItems.isEmpty()
                ? orderItems.get(0).productName()
                : "상품";
    }

    public Long getTotalQuantity() {
        return orderItems != null
                ? orderItems.stream().mapToLong(OrderItemInfo::quantity).sum()
                : 0L;
    }

    // 하위 호환성을 위한 메서드
    public Long getTotalPrice() {
        return finalTotalPrice;
    }

    // 간단한 알림용 메서드들
    public String getOrderSummary() {
        return getFirstProductName() +
                (getOrderItemCount() > 1 ? String.format(" 외 %d개", getOrderItemCount() - 1) : "");
    }
}