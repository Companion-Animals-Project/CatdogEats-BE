package com.team5.catdogeats.orders.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "shipments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Shipments extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_shipments_order"))
    private Orders orders;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_shipments_seller"))
    private Sellers seller;

    @Column(length = 50, nullable = false)
    private String courier;

    @Column(name = "tracking_number", length = 100, nullable = false)
    private String trackingNumber;

    @Column(name = "shipped_at")
    private ZonedDateTime shippedAt;

    @Column(name = "delivered_at")
    private ZonedDateTime deliveredAt;

    // ===== 배송지 정보 필드 (Orders에서 이전) =====
    /**
     * 받는 사람 이름
     * 주문 시점에 입력된 실제 수령인 이름
     */
    @Column(name = "recipient_name", length = 100, nullable = false)
    private String recipientName;

    /**
     * 받는 사람 연락처
     * 주문 시점에 입력된 실제 수령인 연락처
     */
    @Column(name = "recipient_phone", length = 20, nullable = false)
    private String recipientPhone;

    /**
     * 우편번호
     */
    @Column(name = "postal_code", length = 10, nullable = false)
    private String postalCode;

    /**
     * 배송 주소
     * 주문 시점에 입력된 전체 배송 주소
     */
    @Column(name = "shipping_address", length = 500, nullable = false)
    private String shippingAddress;

    /**
     * 상세 주소
     */
    @Column(name = "detail_address", length = 200)
    private String detailAddress;

    /**
     * 배송 요청사항
     * 주문 시점에 입력된 배송 관련 요청사항
     */
    @Column(name = "delivery_note", length = 500)
    private String deliveryNote;

    // ===== 배송지 정보 설정을 위한 편의 메서드 =====
    /**
     * 배송지 정보를 한 번에 설정하는 편의 메서드
     * @param recipientName 받는 사람 이름
     * @param recipientPhone 받는 사람 연락처
     * @param postalCode 우편번호
     * @param streetAddress 기본 주소
     * @param detailAddress 상세 주소
     * @param deliveryNote 배송 요청사항
     */
    public void setShippingInfo(String recipientName, String recipientPhone, String postalCode,
                                String streetAddress, String detailAddress, String deliveryNote) {
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.postalCode = postalCode;
        this.shippingAddress = streetAddress;
        this.detailAddress = detailAddress;
        this.deliveryNote = deliveryNote;
    }

    /**
     * 전체 배송 주소를 조합해서 반환하는 편의 메서드
     * @return 전체 배송 주소 문자열
     */
    public String getFullShippingAddress() {
        if (shippingAddress == null) {
            return "";
        }
        StringBuilder fullAddress = new StringBuilder(shippingAddress);
        if (detailAddress != null && !detailAddress.trim().isEmpty()) {
            fullAddress.append(" ").append(detailAddress);
        }
        return fullAddress.toString().trim();
    }

    // ===== 배송 추적 정보 설정을 위한 편의 메서드 =====
    /**
     * 배송 추적 정보를 설정하는 메서드
     * @param courier 택배사
     * @param trackingNumber 운송장 번호
     */
    public void setTrackingInfo(String courier, String trackingNumber) {
        this.courier = courier;
        this.trackingNumber = trackingNumber;
        this.shippedAt = ZonedDateTime.now();
    }

    /**
     * 배송 완료 처리
     */
    public void markAsDelivered() {
        this.deliveredAt = ZonedDateTime.now();
    }

    /**
     * 배송 시작 여부 확인
     * @return 배송 시작 여부
     */
    public boolean isShipped() {
        return trackingNumber != null && !trackingNumber.trim().isEmpty();
    }

    /**
     * 배송 완료 여부 확인
     * @return 배송 완료 여부
     */
    public boolean isDelivered() {
        return deliveredAt != null;
    }
}
