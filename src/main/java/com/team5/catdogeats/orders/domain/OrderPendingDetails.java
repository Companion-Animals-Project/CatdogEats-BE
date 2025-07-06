package com.team5.catdogeats.orders.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false,
    foreignKey = @ForeignKey(name = "fk_order_pending_details_buyer"))
    private Buyers buyers;

    // 쿠폰 할인 정보
    @Column(name = "original_total_price")
    private Long originalTotalPrice;

    @Column(name = "total_delivery_fee")
    private Long totalDeliveryFee;
    @Column(name = "final_payment_amount")
    private Long finalPaymentAmount;

    // 주문 상품 정보 (JSON 형태로 저장)
    @Lob
    @Column(name = "order_items_json", columnDefinition = "TEXT")
    private String orderItemsJson;

    // 배송지 정보 (JSON 형태로 저장)
    @Lob
    @Column(name = "shipping_address_json", columnDefinition = "TEXT")
    private String shippingAddressJson;

    @Lob
    @Column(name = "applied_coupons_json", columnDefinition = "TEXT")
    private String appliedCouponsJson;
}