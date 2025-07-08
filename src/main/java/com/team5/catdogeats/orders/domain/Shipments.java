package com.team5.catdogeats.orders.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.Users;
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
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_shipments_user"))
    private Users user;

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

    // ===== 비즈니스 메서드 (Address 유틸 기능과 동일) =====

    /**
     * 전체 주소 반환 (Address와 동일한 로직)
     */
    public String getFullAddress() {
        return String.format("%s %s",
                streetAddress != null ? streetAddress : "",
                detailAddress != null ? detailAddress : "").trim();
    }

    /**
     * 우편번호 포함 전체 주소 반환 (Address와 동일한 로직)
     */
    public String getFullAddressWithPostalCode() {
        return String.format("(%s) %s", postalCode, getFullAddress());
    }

    // ===== Builder 패턴 (Address와 동일한 필드명) =====

    /**
     * Builder 생성 메서드
     */
    public static ShipmentsBuilder builder() {
        return new ShipmentsBuilder();
    }

    public static class ShipmentsBuilder {
        private String id;
        private Orders orders;
        private Users user;
        private Sellers seller;
        private String courier;
        private String trackingNumber;
        private ZonedDateTime shippedAt;
        private ZonedDateTime deliveredAt;
        private ZonedDateTime trackingUpdatedAt;
        private ZonedDateTime expectedShipDate;
        private String delayReason;
        private String recipientName;
        private String recipientPhone;
        private String postalCode;           // Address와 동일
        private String streetAddress;        // Address와 동일
        private String detailAddress;        // Address와 동일
        private String deliveryRequest;
        private Boolean isHiddenBySeller = Boolean.FALSE;
        private ZonedDateTime hiddenAt;
        private String shipmentMemo;

        // ===== 기본 Builder 메서드들 =====

        public ShipmentsBuilder id(String id) {
            this.id = id;
            return this;
        }

        public ShipmentsBuilder orders(Orders orders) {
            this.orders = orders;
            return this;
        }

        public ShipmentsBuilder user(Users user) {
            this.user = user;
            return this;
        }

        public ShipmentsBuilder seller(Sellers seller) {
            this.seller = seller;
            return this;
        }

        public ShipmentsBuilder courier(String courier) {
            this.courier = courier;
            return this;
        }

        public ShipmentsBuilder trackingNumber(String trackingNumber) {
            this.trackingNumber = trackingNumber;
            return this;
        }

        public ShipmentsBuilder shippedAt(ZonedDateTime shippedAt) {
            this.shippedAt = shippedAt;
            return this;
        }

        public ShipmentsBuilder deliveredAt(ZonedDateTime deliveredAt) {
            this.deliveredAt = deliveredAt;
            return this;
        }

        public ShipmentsBuilder trackingUpdatedAt(ZonedDateTime trackingUpdatedAt) {
            this.trackingUpdatedAt = trackingUpdatedAt;
            return this;
        }

        public ShipmentsBuilder expectedShipDate(ZonedDateTime expectedShipDate) {
            this.expectedShipDate = expectedShipDate;
            return this;
        }

        public ShipmentsBuilder delayReason(String delayReason) {
            this.delayReason = delayReason;
            return this;
        }

        public ShipmentsBuilder recipientName(String recipientName) {
            this.recipientName = recipientName;
            return this;
        }

        public ShipmentsBuilder recipientPhone(String recipientPhone) {
            this.recipientPhone = recipientPhone;
            return this;
        }

        // ===== Address와 동일한 필드명 Builder 메서드들 =====

        public ShipmentsBuilder postalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        public ShipmentsBuilder streetAddress(String streetAddress) {
            this.streetAddress = streetAddress;
            return this;
        }

        public ShipmentsBuilder detailAddress(String detailAddress) {
            this.detailAddress = detailAddress;
            return this;
        }

        public ShipmentsBuilder deliveryRequest(String deliveryRequest) {
            this.deliveryRequest = deliveryRequest;
            return this;
        }

        public ShipmentsBuilder isHiddenBySeller(Boolean isHiddenBySeller) {
            this.isHiddenBySeller = isHiddenBySeller;
            return this;
        }

        public ShipmentsBuilder hiddenAt(ZonedDateTime hiddenAt) {
            this.hiddenAt = hiddenAt;
            return this;
        }

        public ShipmentsBuilder shipmentMemo(String shipmentMemo) {
            this.shipmentMemo = shipmentMemo;
            return this;
        }

        /**
         * Shipments 객체 생성
         */
        public Shipments build() {
            Shipments shipments = new Shipments();
            shipments.id = this.id;
            shipments.orders = this.orders;
            shipments.user = this.user;
            shipments.seller = this.seller;
            shipments.courier = this.courier;
            shipments.trackingNumber = this.trackingNumber;
            shipments.shippedAt = this.shippedAt;
            shipments.deliveredAt = this.deliveredAt;
            shipments.trackingUpdatedAt = this.trackingUpdatedAt;
            shipments.expectedShipDate = this.expectedShipDate;
            shipments.delayReason = this.delayReason;
            shipments.recipientName = this.recipientName;
            shipments.recipientPhone = this.recipientPhone;
            shipments.postalCode = this.postalCode;
            shipments.streetAddress = this.streetAddress;
            shipments.detailAddress = this.detailAddress;
            shipments.deliveryRequest = this.deliveryRequest;
            shipments.isHiddenBySeller = this.isHiddenBySeller != null ? this.isHiddenBySeller : Boolean.FALSE;
            shipments.hiddenAt = this.hiddenAt;
            shipments.shipmentMemo = this.shipmentMemo;
            return shipments;
        }
    }
}