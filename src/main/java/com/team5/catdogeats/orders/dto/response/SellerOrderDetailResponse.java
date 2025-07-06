package com.team5.catdogeats.orders.dto.response;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 판매자용 주문 상세 응답 DTO
 * API: GET /v1/sellers/orders/{order-number}
 *
 * 판매자가 본인이 판매한 상품이 포함된 주문의 상세 정보를 조회하는 응답 구조
 */
@Builder
public record SellerOrderDetailResponse(
        // 기본 주문 정보
        String orderNumber,                 // 주문 번호
        OrderStatus orderStatus,            // 주문 상태
        ZonedDateTime orderDate,            // 주문일시

        // 배송지 정보
        ShippingAddress shippingAddress,    // 배송지 정보

        // 주문 상품 정보 (해당 판매자 상품만)
        List<SellerOrderDetailItem> orderItems, // 상품 목록
        OrderSummary orderSummary,          // 주문 요약 정보

        // 배송 정보
        ShipmentInfo shipmentInfo,          // 배송 정보

        // 상태 관리 정보
        StatusManagement statusManagement   // 상태 관리 정보
) {

    /**
     * 배송지 정보
     */
    @Builder
    public record ShippingAddress(
            String recipientName,           // 수령인명
            String recipientPhone,          // 수령인 전화번호
            String maskedPhone,             // 마스킹된 전화번호
            String zipCode,                 // 우편번호
            String address,                 // 기본 주소
            String addressDetail,           // 상세 주소
            String fullAddress,             // 전체 주소
            String deliveryRequest          // 배송 요청사항
    ) {

        /**
         * 완전한 배송지 정보인지 확인
         * @return 배송지 정보 완성 여부
         */
        public boolean isComplete() {
            return recipientName != null && !recipientName.trim().isEmpty() &&
                   recipientPhone != null && !recipientPhone.trim().isEmpty() &&
                   address != null && !address.trim().isEmpty() &&
                   addressDetail != null && !addressDetail.trim().isEmpty() &&
                   zipCode != null && !zipCode.trim().isEmpty();
        }
    }

    /**
     * 판매자 주문 상품 상세 정보
     */
    @Builder
    public record SellerOrderDetailItem(
            String productId,               // 상품 ID
            String productName,             // 상품명
            String productImageUrl,         // 상품 이미지 URL
            String productDescription,      // 상품 설명
            Long unitPrice,                 // 단가
            int quantity,                   // 수량
            Long totalPrice,                // 총 가격 (단가 × 수량)
            String productOptions,          // 상품 옵션 정보
            String productSku,              // 상품 SKU
            String categoryName             // 카테고리명
    ) {}

    /**
     * 주문 요약 정보
     */
    @Builder
    public record OrderSummary(
            Long totalProductPrice,         // 상품 총액
            Long deliveryFee,               // 배송비
            Long discountAmount,            // 할인 금액
            Long finalAmount,               // 최종 결제 금액
            int totalItemCount,             // 총 상품 종류 수
            int totalQuantity               // 총 수량
    ) {}

    /**
     * 배송 정보
     */
    @Builder
    public record ShipmentInfo(
            String courier,                 // 택배사
            String trackingNumber,          // 운송장 번호
            ZonedDateTime shippedAt,        // 발송일시
            ZonedDateTime deliveredAt,      // 배송완료일시
            ZonedDateTime trackingUpdatedAt, // 추적정보 갱신일시
            String trackingUrl,             // 배송 추적 URL
            String shipmentMemo             // 배송 메모
    ) {

        /**
         * 배송 시작 여부 확인
         * @return 배송 시작 여부
         */
        public boolean isShipped() {
            return trackingNumber != null &&
                   !trackingNumber.trim().isEmpty() &&
                   shippedAt != null;
        }

        /**
         * 배송 완료 여부 확인
         * @return 배송 완료 여부
         */
        public boolean isDelivered() {
            return deliveredAt != null;
        }

        /**
         * 배송 상태 요약
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
    }

    /**
     * 상태 관리 정보
     */
    @Builder
    public record StatusManagement(
            boolean canChangeStatus,        // 상태 변경 가능 여부
            List<OrderStatus> availableStatuses, // 변경 가능한 상태 목록
            boolean requiresTracking,       // 운송장 등록 필요 여부
            boolean canRegisterTracking,    // 운송장 등록 가능 여부
            boolean canHideOrder,           // 주문 숨김 가능 여부
            String nextAction,              // 다음 수행 작업 안내
            String statusDescription        // 현재 상태 설명
    ) {

        /**
         * 특정 상태로 변경 가능한지 확인
         * @param targetStatus 목표 상태
         * @return 변경 가능 여부
         */
        public boolean canChangeTo(OrderStatus targetStatus) {
            return canChangeStatus &&
                   availableStatuses != null &&
                   availableStatuses.contains(targetStatus);
        }
    }

    /**
     * 성공 응답 생성
     * @param orderNumber 주문 번호
     * @param orderStatus 주문 상태
     * @param orderDate 주문일시
     * @param shippingAddress 배송지 정보
     * @param orderItems 주문 상품 목록
     * @param orderSummary 주문 요약
     * @param shipmentInfo 배송 정보
     * @param statusManagement 상태 관리 정보
     * @return 판매자용 주문 상세 응답
     */
    public static SellerOrderDetailResponse of(
            String orderNumber,
            OrderStatus orderStatus,
            ZonedDateTime orderDate,
            ShippingAddress shippingAddress,
            List<SellerOrderDetailItem> orderItems,
            OrderSummary orderSummary,
            ShipmentInfo shipmentInfo,
            StatusManagement statusManagement
    ) {
        return SellerOrderDetailResponse.builder()
                .orderNumber(orderNumber)
                .orderStatus(orderStatus)
                .orderDate(orderDate)
                .shippingAddress(shippingAddress)
                .orderItems(orderItems)
                .orderSummary(orderSummary)
                .shipmentInfo(shipmentInfo)
                .statusManagement(statusManagement)
                .build();
    }

    /**
     * 배송 시작 여부 확인
     * @return 배송 시작 여부
     */
    public boolean isShipped() {
        return shipmentInfo != null && shipmentInfo.isShipped();
    }

    /**
     * 배송 완료 여부 확인
     * @return 배송 완료 여부
     */
    public boolean isDelivered() {
        return shipmentInfo != null && shipmentInfo.isDelivered();
    }

    /**
     * 상태 변경 가능 여부 확인
     * @return 상태 변경 가능 여부
     */
    public boolean canChangeStatus() {
        return statusManagement != null && statusManagement.canChangeStatus();
    }

    /**
     * 운송장 등록 가능 여부 확인
     * @return 운송장 등록 가능 여부
     */
    public boolean canRegisterTracking() {
        return statusManagement != null && statusManagement.canRegisterTracking();
    }

    /**
     * 주문 숨김 가능 여부 확인
     * @return 주문 숨김 가능 여부
     */
    public boolean canHideOrder() {
        return statusManagement != null && statusManagement.canHideOrder();
    }

    /**
     * 총 주문 금액 반환
     * @return 총 주문 금액
     */
    public Long getTotalAmount() {
        return orderSummary != null ? orderSummary.finalAmount() : 0L;
    }

    /**
     * 총 상품 수량 반환
     * @return 총 상품 수량
     */
    public Integer getTotalQuantity() {
        return orderSummary != null ? orderSummary.totalQuantity() : 0;
    }

    /**
     * 배송지가 완전한지 확인
     * @return 배송지 정보 완성 여부
     */
    public boolean hasCompleteShippingAddress() {
        return shippingAddress != null && shippingAddress.isComplete();
    }

    /**
     * 주문 상품이 있는지 확인
     * @return 주문 상품 존재 여부
     */
    public boolean hasOrderItems() {
        return orderItems != null && !orderItems.isEmpty();
    }
}