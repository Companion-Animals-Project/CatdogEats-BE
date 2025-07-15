package com.team5.catdogeats.orders.dto.response;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 구매자용 배송 정보 상세 조회 응답 DTO
 * API: GET /v1/buyers/shipments/{order-number}
 */
public record BuyerShipmentDetailResponse(
        String orderNumber,
        OrderStatus deliveryStatus,
        ZonedDateTime arrivalDate,
        TrackingInfo trackingInfo,
        RecipientInfo recipientInfo,
        List<TrackingLog> trackingLogs
) {

    /**
     * 운송장 정보
     */
    public record TrackingInfo(
            String trackingNumber,
            String carrierName
    ) {

        /**
         * 정적 팩토리 메서드
         */
        public static TrackingInfo of(String trackingNumber, String carrierName) {
            return new TrackingInfo(trackingNumber, carrierName);
        }
    }

    /**
     * 수취인 정보
     */
    public record RecipientInfo(
            String recipientName,
            String recipientPhone,
            String fullAddress,
            String deliveryRequest
    ) {

        /**
         * 정적 팩토리 메서드 - 주소 정보 조합
         */
        public static RecipientInfo of(
                String recipientName,
                String recipientPhone,
                String postalCode,
                String streetAddress,
                String detailAddress,
                String deliveryRequest) {

            String fullAddress = buildFullAddress(postalCode, streetAddress, detailAddress);

            return new RecipientInfo(
                    recipientName,
                    recipientPhone,
                    fullAddress,
                    deliveryRequest
            );
        }

        /**
         * 전체 주소 조합 헬퍼 메서드
         */
        private static String buildFullAddress(String postalCode, String streetAddress, String detailAddress) {
            StringBuilder address = new StringBuilder();

            if (postalCode != null && !postalCode.trim().isEmpty()) {
                address.append("(").append(postalCode).append(") ");
            }

            if (streetAddress != null && !streetAddress.trim().isEmpty()) {
                address.append(streetAddress);
            }

            if (detailAddress != null && !detailAddress.trim().isEmpty()) {
                address.append(" ").append(detailAddress);
            }

            return address.toString().trim();
        }
    }

    /**
     * 배송 추적 로그
     */
    public record TrackingLog(
            ZonedDateTime timestamp,
            String status,
            String location,
            String description
    ) {

        /**
         * 정적 팩토리 메서드
         */
        public static TrackingLog of(
                ZonedDateTime timestamp,
                String status,
                String location,
                String description) {

            return new TrackingLog(timestamp, status, location, description);
        }
    }

    /**
     * 정적 팩토리 메서드 - 배송 완료된 주문용
     */
    public static BuyerShipmentDetailResponse withArrivalDate(
            String orderNumber,
            ZonedDateTime arrivalDate,
            TrackingInfo trackingInfo,
            RecipientInfo recipientInfo,
            List<TrackingLog> trackingLogs) {

        return new BuyerShipmentDetailResponse(
                orderNumber,
                OrderStatus.DELIVERED,
                arrivalDate,
                trackingInfo,
                recipientInfo,
                trackingLogs
        );
    }

    /**
     * 정적 팩토리 메서드 - 배송 중인 주문용
     */
    public static BuyerShipmentDetailResponse withDeliveryStatus(
            String orderNumber,
            OrderStatus deliveryStatus,
            TrackingInfo trackingInfo,
            RecipientInfo recipientInfo,
            List<TrackingLog> trackingLogs) {

        return new BuyerShipmentDetailResponse(
                orderNumber,
                deliveryStatus,
                null,
                trackingInfo,
                recipientInfo,
                trackingLogs
        );
    }
}