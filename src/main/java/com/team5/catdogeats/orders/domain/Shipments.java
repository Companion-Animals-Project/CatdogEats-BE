package com.team5.catdogeats.orders.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * 주문 1건‑당 배송정보 1건을 관리하는 엔티티
 */
@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Shipments extends BaseEntity {

    /* ===== 기본 식별자 ===== */
    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /* ===== 연관관계 ===== */
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

    /* ===== 운송장(Tracking) ===== */
    @Column(length = 50)
    private String courier;                       // 택배사명(표기 그대로)

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;                // 운송장 번호

    @Column(name = "shipped_at")
    private ZonedDateTime shippedAt;              // 발송 시각

    @Column(name = "delivered_at")
    private ZonedDateTime deliveredAt;            // 배송 완료 시각

    @Column(name = "tracking_updated_at")
    private ZonedDateTime trackingUpdatedAt;      // 마지막 추적 정보 갱신 시각

    /* ===== 배송지(Address) ===== */
    @Column(name = "recipient_name", length = 100, nullable = false)
    private String recipientName;                 // 수령인

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;                // 수령인 전화번호

    @Column(name = "zip_code", length = 10)
    private String zipCode;                       // 우편번호

    @Column(name = "address", length = 200)
    private String address;                       // 기본 주소

    @Column(name = "address_detail", length = 100)
    private String addressDetail;                 // 상세 주소

    @Column(name = "delivery_request", length = 200)
    private String deliveryRequest;               // 요청사항

    /* ===== 판매자 관리 기능 ===== */
    @Column(name = "is_hidden_by_seller")
    @Builder.Default
    private Boolean isHiddenBySeller = Boolean.FALSE;  // 판매자 목록 숨김 여부

    @Column(name = "hidden_at")
    private ZonedDateTime hiddenAt;                    // 숨김 처리 시각

    /* ----------------------------------------------------------------------------------
     *                                 편의 메서드
     * ----------------------------------------------------------------------------------
     */

    /* ---------- 배송지 ---------- */

    /**
     * 배송지 정보를 한 번에 설정
     */
    public void setShippingInfo(String recipientName,
                                String recipientPhone,
                                String zipCode,
                                String address,
                                String addressDetail,
                                String deliveryRequest) {
        this.recipientName   = recipientName;
        this.recipientPhone  = recipientPhone;
        this.zipCode         = zipCode;
        this.address         = address;
        this.addressDetail   = addressDetail;
        this.deliveryRequest = deliveryRequest;
    }

    /**
     * (우편번호) 기본주소 + 상세주소 를 합친 전체 주소
     */
    public String getFullShippingAddress() {
        StringBuilder sb = new StringBuilder();
        if (zipCode != null && !zipCode.isBlank()) {
            sb.append("(").append(zipCode).append(") ");
        }
        if (address != null) {
            sb.append(address);
        }
        if (addressDetail != null && !addressDetail.isBlank()) {
            sb.append(" ").append(addressDetail);
        }
        return sb.toString().trim();
    }

    /* ---------- 운송장 관리 ---------- */

    /** 운송장 정보 등록(현재 시각을 발송 시각으로) */
    public void setTrackingInfo(String courier, String trackingNumber) {
        this.courier          = courier;
        this.trackingNumber   = trackingNumber;
        this.shippedAt        = ZonedDateTime.now();
        this.trackingUpdatedAt = ZonedDateTime.now();
    }

    /** 운송장 정보 등록(발송 시각 직접 지정) */
    public void setTrackingInfo(String courier, String trackingNumber, ZonedDateTime shippedAt) {
        this.courier          = courier;
        this.trackingNumber   = trackingNumber;
        this.shippedAt        = shippedAt;
        this.trackingUpdatedAt = ZonedDateTime.now();
    }

    /** 배송 완료 처리(현재 시각) */
    public void markAsDelivered() {
        this.deliveredAt       = ZonedDateTime.now();
        this.trackingUpdatedAt = ZonedDateTime.now();
    }

    /** 배송 완료 처리(완료 시각 직접 지정) */
    public void markAsDelivered(ZonedDateTime deliveredAt) {
        this.deliveredAt       = deliveredAt;
        this.trackingUpdatedAt = ZonedDateTime.now();
    }

    /** 스마트택배 API 호출 후 추적 정보 갱신 시각만 업데이트 */
    public void updateTrackingTimestamp() {
        this.trackingUpdatedAt = ZonedDateTime.now();
    }

    /* ---------- 상태 확인 ---------- */

    public boolean isShipped() {
        return trackingNumber != null && !trackingNumber.trim().isEmpty()
                && courier != null && !courier.trim().isEmpty()
                && shippedAt != null;
    }

    public boolean isDelivered() {
        return deliveredAt != null;
    }

    public boolean hasTrackingInfo() {
        return trackingNumber != null && !trackingNumber.trim().isEmpty();
    }

    public boolean isHiddenBySeller() {
        return Boolean.TRUE.equals(this.isHiddenBySeller);
    }

    /* ---------- 판매자 목록 숨김 ---------- */

    /** 판매자 목록에서 숨김 */
    public void hideFromSellerList() {
        this.isHiddenBySeller = Boolean.TRUE;
        this.hiddenAt         = ZonedDateTime.now();
    }

    /** 숨김 해제 */
    public void showInSellerList() {
        this.isHiddenBySeller = Boolean.FALSE;
        this.hiddenAt         = null;
    }

    /* ---------- 기타 유틸 ---------- */

    /** 운송장 형식 검증(영·숫자·하이픈, 8~50자) */
    public boolean hasValidTrackingNumber() {
        if (!hasTrackingInfo()) return false;
        String n = trackingNumber.trim();
        return n.length() >= 8 && n.length() <= 50 && n.matches("^[A-Za-z0-9\\-]+$");
    }

    /** 배송지 정보 완성도 확인 */
    public boolean hasCompleteAddress() {
        return recipientName  != null && !recipientName.trim().isEmpty() &&
                recipientPhone != null && !recipientPhone.trim().isEmpty() &&
                zipCode        != null && !zipCode.trim().isEmpty() &&
                address        != null && !address.trim().isEmpty() &&
                addressDetail  != null && !addressDetail.trim().isEmpty();
    }

    /** 전화번호 마스킹(010-****-1234 등) */
    public String getMaskedPhone() {
        if (recipientPhone == null || recipientPhone.length() < 4) return "***-****-****";
        String digits = recipientPhone.replaceAll("[^0-9]", "");
        if (digits.length() == 11) {   // 01012345678
            return digits.substring(0, 3) + "-****-" + digits.substring(7);
        } else if (digits.length() == 10) { // 0111234567
            return digits.substring(0, 3) + "-***-" + digits.substring(6);
        }
        return digits.substring(0, 3) + "****";
    }

    /** 간단 상태 텍스트 */
    public String getShipmentStatusSummary() {
        if (isDelivered()) return "배송완료";
        if (isShipped())   return "배송중";
        return "배송준비";
    }
}
