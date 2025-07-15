package com.team5.catdogeats.orders.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Shipments extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_shipments_order"))
    private Orders orders;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_shipments_buyer"))
    private Buyers buyers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id",
            foreignKey = @ForeignKey(name = "fk_shipments_seller"))
    private Sellers seller;

    // ===== 배송 추적 정보 =====
    @Column(length = 50)
    private String courier;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private ZonedDateTime shippedAt;

    @Column(name = "delivered_at")
    private ZonedDateTime deliveredAt;

    @Column(name = "tracking_updated_at")
    private ZonedDateTime trackingUpdatedAt;

    // ===== 판매자 배송 관리 필드들 =====
    /**
     * 예상 배송일
     */
    @Column(name = "expected_ship_date")
    private ZonedDateTime expectedShipDate;
    /**
     * 출고 지연 여부
     */
    @Column(name = "is_delayed")
    @Builder.Default
    private Boolean isDelayed = Boolean.FALSE;
    /**
     * 배송 지연 사유
     */
    @Column(name = "delay_reason", length = 500)
    private String delayReason;

    // ===== 배송지 정보 필드들 (Address 도메인과 동일한 필드명) =====
    /**
     * 받는 사람 이름
     */
    @Column(name = "recipient_name", length = 100, nullable = false)
    private String recipientName;

    /**
     * 받는 사람 전화번호
     */
    @Column(name = "recipient_phone", length = 20, nullable = false)
    private String recipientPhone;

    /**
     * 우편번호 (Address와 동일: postalCode)
     */
    @Column(name = "postal_code", length = 20, nullable = false)
    private String postalCode;

    /**
     * 도로명 주소 (Address와 동일: streetAddress)
     */
    @Column(name = "street_address", length = 200, nullable = false)
    private String streetAddress;

    /**
     * 상세 주소 (Address와 동일: detailAddress)
     */
    @Column(name = "detail_address", length = 200)
    private String detailAddress;

    /**
     * 배송 요청사항
     */
    @Column(name = "delivery_request", length = 500)
    private String deliveryRequest;

    // ===== 판매자 관리 기능 필드들 =====
    /**
     * 판매자에 의한 숨김 여부
     */
    @Column(name = "is_hidden_by_seller")
    @Builder.Default
    private Boolean isHiddenBySeller = Boolean.FALSE;

    /**
     * 판매자에 의한 숨김 처리 시각
     */
    @Column(name = "hidden_at")
    private ZonedDateTime hiddenAt;

    /**
     * 배송 메모
     */
    @Column(name = "shipment_memo", length = 500)
    private String shipmentMemo;
}