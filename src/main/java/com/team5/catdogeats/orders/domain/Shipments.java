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

    // ===== 새로 추가된 필드들 =====
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

    // ===== 새로 추가된 메서드들 =====

    /**
     * postalCode getter (zipCode와 매핑)
     */
    public String getPostalCode() {
        return this.zipCode;
    }

    /**
     * 전체 주소 반환
     */
    public String getFullAddress() {
        StringBuilder fullAddress = new StringBuilder();
        if (address != null && !address.trim().isEmpty()) {
            fullAddress.append(address);
        }
        if (addressDetail != null && !addressDetail.trim().isEmpty()) {
            if (!fullAddress.isEmpty()) {
                fullAddress.append(" ");
            }
            fullAddress.append(addressDetail);
        }
        return fullAddress.toString();
    }

    /**
     * 도시 정보 추출 (주소에서 첫 번째 공백 전까지)
     */
    public String getCity() {
        if (address == null || address.trim().isEmpty()) {
            return "";
        }
        String[] parts = address.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    /**
     * 구/군 정보 추출 (주소에서 두 번째 부분)
     */
    public String getDistrict() {
        if (address == null || address.trim().isEmpty()) {
            return "";
        }
        String[] parts = address.trim().split("\\s+");
        return parts.length > 1 ? parts[1] : "";
    }

    /**
     * 동/면 정보 추출 (주소에서 세 번째 부분)
     */
    public String getNeighborhood() {
        if (address == null || address.trim().isEmpty()) {
            return "";
        }
        String[] parts = address.trim().split("\\s+");
        return parts.length > 2 ? parts[2] : "";
    }

    /**
     * 배송 완료 여부 확인
     */
    public boolean isShipped() {
        return shippedAt != null;
    }

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
     * 배송 완료 처리
     */
    public void markAsDelivered() {
        this.deliveredAt = ZonedDateTime.now();
        this.trackingUpdatedAt = ZonedDateTime.now();
    }

    /**
     * 판매자에 의한 숨김 처리
     */
    public void hideFromSeller() {
        this.isHiddenBySeller = Boolean.TRUE;
        this.hiddenAt = ZonedDateTime.now();
    }

    /**
     * 판매자에 의한 숨김 해제
     */
    public void showToSeller() {
        this.isHiddenBySeller = Boolean.FALSE;
        this.hiddenAt = null;
    }

    /**
     * 배송 메모 업데이트
     */
    public void updateShipmentMemo(String memo) {
        this.shipmentMemo = memo;
    }
}