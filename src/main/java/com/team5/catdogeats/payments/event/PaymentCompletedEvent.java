package com.team5.catdogeats.payments.event;

import com.team5.catdogeats.coupons.domain.enums.DiscountType;
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

        // ===== 쿠폰 할인 정보 확장 =====
        Long originalTotalPrice,
        DiscountType couponType,           // 쿠폰 타입 추가
        Double couponDiscountRate,         // 기존 필드 유지
        Long couponDiscountAmount,         // 정액 할인 금액 추가

        // 이벤트 발생 시각
        ZonedDateTime eventOccurredAt
) {

    /**
     * 단일 정적 팩토리 메서드 (모든 경우를 처리)
     *
     * @param couponType null이면 기존 방식, 값이 있으면 새 방식
     * @param couponDiscountRate 정률 할인률 (couponType이 null이거나 PERCENT일 때 사용)
     * @param couponDiscountAmount 정액 할인 금액 (couponType이 AMOUNT일 때 사용)
     */
    public static PaymentCompletedEvent of(
            String orderId,
            String orderNumber,
            String userId,
            String userProvider,
            String userProviderId,
            String paymentId,
            String tossPaymentKey,
            Long finalAmount,
            List<OrderItemInfo> orderItems,
            OrderCreateRequest.ShippingAddressRequest shippingAddress,
            Long originalTotalPrice,
            DiscountType couponType,         // null 허용
            Double couponDiscountRate,       // null 허용
            Long couponDiscountAmount) {     // null 허용

        return new PaymentCompletedEvent(
                orderId, orderNumber, userId, userProvider, userProviderId,
                paymentId, tossPaymentKey, finalAmount,
                orderItems, shippingAddress,
                originalTotalPrice,
                couponType,
                couponDiscountRate,
                couponDiscountAmount,
                ZonedDateTime.now()
        );
    }

    /**
     * 편의 메서드 - 기존 방식 호출을 위한 오버로딩
     * 기존 코드 수정을 최소화하기 위해 제공
     */
    public static PaymentCompletedEvent of(
            String orderId,
            String orderNumber,
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

        // 내부적으로 메인 팩토리 메서드 호출
        return of(
                orderId, orderNumber, userId, userProvider, userProviderId,
                paymentId, tossPaymentKey, finalAmount,
                orderItems, shippingAddress,
                originalTotalPrice,
                null,                    // couponType = null (기존 방식)
                couponDiscountRate,
                null                     // couponDiscountAmount = null
        );
    }

    // ===== 비즈니스 로직 메서드들 =====

    /**
     * 쿠폰 할인 적용 여부 확인
     */
    public boolean isCouponApplied() {
        if (couponType == null) {
            // 기존 방식
            return couponDiscountRate != null && couponDiscountRate > 0;
        }

        return switch (couponType) {
            case PERCENT -> couponDiscountRate != null && couponDiscountRate > 0;
            case AMOUNT -> couponDiscountAmount != null && couponDiscountAmount > 0;
        };
    }

    /**
     * 실제 할인 금액 계산
     */
    public Long getDiscountAmount() {
        if (!isCouponApplied() || originalTotalPrice == null) {
            return 0L;
        }

        if (couponType == null) {
            // 기존 방식 (정률 할인)
            return Math.round(originalTotalPrice * (couponDiscountRate / 100.0));
        }

        return switch (couponType) {
            case PERCENT -> Math.round(originalTotalPrice * (couponDiscountRate / 100.0));
            case AMOUNT -> couponDiscountAmount;
        };
    }

    /**
     * 쿠폰 설명 문자열 (알림용)
     */
    public String getCouponDescription() {
        if (!isCouponApplied()) {
            return "";
        }

        if (couponType == null) {
            return String.format("%.1f%% 할인", couponDiscountRate);
        }

        return switch (couponType) {
            case PERCENT -> String.format("%.1f%% 할인", couponDiscountRate);
            case AMOUNT -> String.format("%,d원 할인", couponDiscountAmount);
        };
    }

    public int getOrderItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    public String getFirstProductName() {
        return orderItems != null && !orderItems.isEmpty()
                ? orderItems.get(0).productName()
                : "상품";
    }
}