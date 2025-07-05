package com.team5.catdogeats.orders.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import com.team5.catdogeats.coupons.domain.enums.DiscountType;

/**
 * 주문 대기 상세 정보 엔티티
 * 결제 완료 전까지 주문 상품 정보와 배송지 정보를 임시로 저장합니다.
 * 결제 완료 시 이 정보를 사용하여 OrderItems와 Shipments를 생성합니다.
 */
@Entity
@Table(name = "order_pending_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderPendingDetails extends BaseEntity {

    @Id
    @Column(name = "order_id", length = 36)
    private String orderId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    @MapsId
    private Orders orders;

    // 사용자 인증 정보 (PaymentCompletedEvent에서 필요)
    @Column(name = "user_provider", length = 50)
    private String userProvider;

    @Column(name = "user_provider_id", length = 100)
    private String userProviderId;

    // 쿠폰 할인 정보
    @Column(name = "original_total_price")
    private Long originalTotalPrice;

    /**
     * 쿠폰 할인 타입 (PERCENT: 정률, AMOUNT: 정액)
     * null인 경우 기존 방식(정률 할인)으로 처리
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", length = 10)
    private DiscountType couponType;

    @Column(name = "coupon_discount_rate")
    private Double couponDiscountRate;  // 기존 필드 유지

    /**
     * 정액 할인 금액 (원)
     * couponType이 AMOUNT일 때 사용
     */
    @Column(name = "coupon_discount_amount")
    private Long couponDiscountAmount;

    // 주문 상품 정보 (JSON 형태로 저장)
    @Lob
    @Column(name = "order_items_json", columnDefinition = "TEXT")
    private String orderItemsJson;

    // 배송지 정보 (JSON 형태로 저장)
    @Lob
    @Column(name = "shipping_address_json", columnDefinition = "TEXT")
    private String shippingAddressJson;

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
}