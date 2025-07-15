package com.team5.catdogeats.orders.dto.response;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 판매자용 주문 목록 응답 DTO
 * API: GET /v1/sellers/orders/list
 */
@Builder
public record SellerOrderListResponse(
        List<SellerOrderSummary> orders,
        int currentPage,
        int totalPages,
        long totalElements,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious,
        String searchType,
        String searchKeyword,
        OrderStatus filterStatus
) {

    /**
     * 판매자용 주문 요약 정보
     */
    @Builder
    public record SellerOrderSummary(
            String orderNumber,
            OrderStatus orderStatus,
            ZonedDateTime orderDate,
            String buyerName,
            String maskedBuyerName,
            List<SellerOrderItem> orderItems,
            OrderSummaryInfo orderSummary,
            ShipmentBasicInfo shipmentInfo,
            Boolean isDelayed,        // 📍 추가
            String delayReason
    ) {}

    /**
     * 판매자용 주문 상품 정보
     */
    @Builder
    public record SellerOrderItem(
            String orderItemId,         // ===== 컴파일 오류 해결 =====
            String productId,
            String productName,
            String productImage,
            Integer quantity,
            Long unitPrice,
            Long totalPrice
    ) {}

    /**
     * 주문 요약 정보 (목록용)
     */
    @Builder
    public record OrderSummaryInfo(
            int itemCount,
            Long totalAmount
    ) {}

    /**
     * 배송 기본 정보 (목록용)
     */
    @Builder
    public record ShipmentBasicInfo(
            String courier,
            String trackingNumber,
            boolean isShipped,
            ZonedDateTime shippedAt
    ) {}
}