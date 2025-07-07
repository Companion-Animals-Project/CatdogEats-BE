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

    // ===== 배송지 정보 필드들 (DB 스키마와 일치) =====
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
     * 우편번호
     */
    @Column(name = "zip_code", length = 10, nullable = false)
    private String zipCode;

    /**
     * 기본 주소
     */
    @Column(name = "address", length = 500, nullable = false)
    private String address;

    /**
     * 상세 주소
     */
    @Column(name = "address_detail", length = 200)
    private String addressDetail;

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

    // ===== 운송장 관리 편의 메서드들 =====

    /**
     * 운송장 정보 설정
     * @param courier 택배사명
     * @param trackingNumber 운송장 번호
     */
    public void setTrackingInfo(String courier, String trackingNumber) {
        this.courier = courier;
        this.trackingNumber = trackingNumber;
        this.shippedAt = ZonedDateTime.now();
        this.trackingUpdatedAt = ZonedDateTime.now();
    }

    /**
     * 운송장 정보 설정 (발송 시각 직접 지정)
     * @param courier 택배사명
     * @param trackingNumber 운송장 번호
     * @param shippedAt 발송 시각
     */
    public void setTrackingInfo(String courier, String trackingNumber, ZonedDateTime shippedAt) {
        this.courier = courier;
        this.trackingNumber = trackingNumber;
        this.shippedAt = shippedAt;
        this.trackingUpdatedAt = ZonedDateTime.now();
    }

    /**
     * 배송 완료 처리
     */
    public void markAsDelivered() {
        this.deliveredAt = ZonedDateTime.now();
        this.trackingUpdatedAt = ZonedDateTime.now();
    }

    /**
     * 배송 완료 처리 (완료 시각 직접 지정)
     * @param deliveredAt 배송 완료 시각
     */
    public void markAsDelivered(ZonedDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
        this.trackingUpdatedAt = ZonedDateTime.now();
    }

    /**
     * 배송 추적 정보 업데이트
     */
    public void updateTrackingTimestamp() {
        this.trackingUpdatedAt = ZonedDateTime.now();
    }
    public String getFullShippingAddress() {
        return String.format("%s %s %s", address, addressDetail != null ? addressDetail : "", zipCode);
    }

    public String getDeliveryNote() {
        return deliveryRequest;
    }

    public void setExpectedShipDate(ZonedDateTime expectedShipDate) {
        this.expectedShipDate = expectedShipDate;
    }

    public void setDelayReason(String delayReason) {
        this.delayReason = delayReason;
    }

    public String getFullAddress() {
        return getFullShippingAddress();
    }
    /**
     * 운송장이 등록된 상태인지 확인
     * @return 운송장 등록 여부
     */
    public boolean isShipped() {
        return trackingNumber != null && !trackingNumber.trim().isEmpty();
    }

    /**
     * 배송이 완료된 상태인지 확인
     * @return 배송 완료 여부
     */
    public boolean isDelivered() {
        return deliveredAt != null;
    }

    /**
     * 배송 추적 정보가 있는지 확인
     * @return 추적 정보 존재 여부
     */
    public boolean hasTrackingInfo() {
        return trackingNumber != null && !trackingNumber.trim().isEmpty();
    }

    /**
     * 판매자에 의해 숨김 처리된 상태인지 확인
     * @return 숨김 처리 여부
     */
    public boolean isHiddenBySeller() {
        return Boolean.TRUE.equals(this.isHiddenBySeller);
    }

    /**
     * 판매자용 목록에서 숨김 처리
     */
    public void hideFromSellerList() {
        this.isHiddenBySeller = Boolean.TRUE;
        this.hiddenAt = ZonedDateTime.now();
    }

    /**
     * 판매자용 목록에서 숨김 해제
     */
    public void showInSellerList() {
        this.isHiddenBySeller = Boolean.FALSE;
        this.hiddenAt = null;
    }

    /**
     * 연락처 마스킹 처리
     * @return 마스킹된 전화번호
     */
    public String getMaskedPhone() {
        if (recipientPhone == null || recipientPhone.length() < 4) {
            return "***-****-****";
        }

        String phone = recipientPhone.replaceAll("[^0-9]", "");
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "-****-" + phone.substring(7);
        } else if (phone.length() == 10) {
            return phone.substring(0, 3) + "-***-" + phone.substring(6);
        }

        return phone.substring(0, 3) + "****";
    }

    /**
     * 배송 상태 요약 정보
     * @return 배송 상태 설명
     */
    public String getShipmentStatusSummary() {
        if (isDelivered()) {
            return "배송완료";
        } else if (isShipped()) {
            return "배송중";
        } else {
            return "배송준비";
        }
    }

    /**
     * 배송지 정보가 완전한지 확인
     * @return 배송지 정보 완성 여부
     */
    public boolean hasCompleteAddress() {
        return recipientName != null && !recipientName.trim().isEmpty() &&
                recipientPhone != null && !recipientPhone.trim().isEmpty() &&
                address != null && !address.trim().isEmpty() &&
                addressDetail != null && !addressDetail.trim().isEmpty() &&
                zipCode != null && !zipCode.trim().isEmpty();
    }

    /**
     * 운송장 번호가 유효한 형식인지 확인
     * @return 유효한 운송장 번호 형식인지 여부
     */
    public boolean hasValidTrackingNumber() {
        if (!hasTrackingInfo()) {
            return false;
        }

        String normalized = trackingNumber.trim();
        return normalized.length() >= 8 &&
                normalized.length() <= 50 &&
                normalized.matches("^[A-Za-z0-9\\-]+$");
    }
}