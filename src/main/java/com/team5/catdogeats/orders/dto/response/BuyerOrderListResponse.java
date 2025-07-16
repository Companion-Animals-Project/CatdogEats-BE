package com.team5.catdogeats.orders.dto.response;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 구매자용 배송 정보 목록 조회 응답 DTO
 * API: GET /v1/buyers/orders/list
 */
public record BuyerOrderListResponse(
        List<BuyerOrderSummary> orders,
        int currentPage,
        int totalPages,
        long totalElements,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
) {

    /**
     * 구매자용 주문 요약 정보
     */
    public record BuyerOrderSummary(
            String orderNumber,
            ZonedDateTime orderDate,
            OrderStatus deliveryStatus,
            ZonedDateTime arrivalDate,
            String orderItemsInfo,
            Long totalPrice
    ) {

        /**
         * 정적 팩토리 메서드 - 배송 완료된 주문용
         */
        public static BuyerOrderSummary withArrivalDate(
                String orderNumber,
                ZonedDateTime orderDate,
                ZonedDateTime arrivalDate,
                String orderItemsInfo,
                Long totalPrice) {

            return new BuyerOrderSummary(
                    orderNumber,
                    orderDate,
                    OrderStatus.DELIVERED,
                    arrivalDate,
                    orderItemsInfo,
                    totalPrice
            );
        }

        /**
         * 정적 팩토리 메서드 - 배송 중인 주문용
         */
        public static BuyerOrderSummary withDeliveryStatus(
                String orderNumber,
                ZonedDateTime orderDate,
                OrderStatus deliveryStatus,
                String orderItemsInfo,
                Long totalPrice) {

            return new BuyerOrderSummary(
                    orderNumber,
                    orderDate,
                    deliveryStatus,
                    null,
                    orderItemsInfo,
                    totalPrice
            );
        }
    }

    /**
     * 정적 팩토리 메서드 - 페이징 정보 포함
     */
    public static BuyerOrderListResponse of(
            List<BuyerOrderSummary> orders,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize,
            boolean hasNext,
            boolean hasPrevious) {

        return new BuyerOrderListResponse(
                orders,
                currentPage,
                totalPages,
                totalElements,
                pageSize,
                hasNext,
                hasPrevious
        );
    }
}