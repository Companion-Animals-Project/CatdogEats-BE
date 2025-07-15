package com.team5.catdogeats.orders.dto.response;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 판매자용 주문 상세 응답 DTO
 * API: GET /v1/sellers/orders/{order-number}
 */
@Builder
public record SellerOrderDetailResponse(
        String orderNumber,
        OrderStatus orderStatus,
        ZonedDateTime orderDate,
        ShippingAddress shippingAddress,
        List<SellerOrderDetailItem> orderItems,
        OrderSummary orderSummary,
        ShipmentInfo shipmentInfo,
        StatusManagement statusManagement,
        Boolean isDelayed,        // 📍 추가: 출고 지연 여부
        String delayReason        // 📍 추가: 출고 지연 사유
) {

    /**
     * 판매자용 주문 상품 상세 정보
     */
    @Builder
    public record SellerOrderDetailItem(
            String orderItemId,         // ===== 컴파일 오류 해결 =====
            String productId,
            String productName,
            String productImage,
            Integer quantity,
            Long unitPrice,
            Long totalPrice,
            String vendorName
    ) {}

    /**
     * 주문 요약 정보
     */
    @Builder
    public record OrderSummary(
            int itemCount,              // ===== 컴파일 오류 해결 =====
            Long totalProductPrice,
            Long deliveryFee,
            Long totalAmount
    ) {}

    /**
     * 배송 정보
     */
    @Builder
    public record ShipmentInfo(
            String courier,
            String trackingNumber,
            ZonedDateTime shippedAt,
            ZonedDateTime deliveredAt,
            boolean isShipped,          // ===== 컴파일 오류 해결 =====
            String shipmentMemo
    ) {}

    /**
     * 배송지 정보
     */
    @Builder
    public record ShippingAddress(
            String recipientName,
            String recipientPhone,
            String maskedPhone,
            String zipCode,
            String address,
            String addressDetail,
            String fullAddress,
            String deliveryRequest
    ) {}

    /**
     * 상태 관리 정보
     */
    @Builder
    public record StatusManagement(
            boolean canChangeStatus,
            List<OrderStatus> availableStatuses,
            boolean canRegisterTracking,
            String statusDescription
    ) {}

    // ===== 편의 메서드 =====
    public Long getTotalAmount() {
        return orderSummary != null ? orderSummary.totalAmount() : 0L;
    }
}