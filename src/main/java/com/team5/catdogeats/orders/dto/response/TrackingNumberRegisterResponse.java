package com.team5.catdogeats.orders.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team5.catdogeats.orders.domain.enums.CourierCompany;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * 운송장 번호 등록 응답 DTO
 * API: POST /v1/sellers/orders/tracking-number
 * 판매자의 운송장 번호 등록 요청 처리 결과를 담는 응답 구조
 */
@Builder
public record TrackingNumberRegisterResponse(
        String orderNumber,
        String trackingNumber,
        CourierCompany courierCompany,
        OrderStatus orderStatus,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        ZonedDateTime shippedAt,

        String message,
        ShipmentInfo shipmentInfo
) {

    /**
     * 배송 정보
     */
    @Builder
    public record ShipmentInfo(
            String courier,             // 택배사명
            String trackingNumber,      // 운송장 번호
            String shipmentMemo,        // 배송 메모

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
            ZonedDateTime shippedAt     // 배송 시작 시간
    ) {
        /**
         * 배송 정보 생성 헬퍼 메서드
         */
        public static ShipmentInfo of(String courier, String trackingNumber, String shipmentMemo, ZonedDateTime shippedAt) {
            return ShipmentInfo.builder()
                    .courier(courier)
                    .trackingNumber(trackingNumber)
                    .shipmentMemo(shipmentMemo)
                    .shippedAt(shippedAt)
                    .build();
        }
    }

    /**
     * 성공 응답 생성 헬퍼 메서드
     */
    public static TrackingNumberRegisterResponse success(
            String orderNumber,
            String trackingNumber,
            CourierCompany courierCompany,
            OrderStatus orderStatus,
            ZonedDateTime shippedAt,
            String message,
            ShipmentInfo shipmentInfo) {

        return TrackingNumberRegisterResponse.builder()
                .orderNumber(orderNumber)
                .trackingNumber(trackingNumber)
                .courierCompany(courierCompany)
                .orderStatus(orderStatus)
                .shippedAt(shippedAt)
                .message(message)
                .shipmentInfo(shipmentInfo)
                .build();
    }

    /**
     * 운송장 번호 마스킹 처리
     * @return 마스킹된 운송장 번호
     */
    public String getMaskedTrackingNumber() {
        if (trackingNumber == null || trackingNumber.length() <= 6) {
            return "***";
        }
        return trackingNumber.substring(0, 3) + "***" + trackingNumber.substring(trackingNumber.length() - 3);
    }

    /**
     * 배송 시작 여부 확인
     * @return 배송이 시작되었는지 여부
     */
    public boolean isShipped() {
        return shippedAt != null && orderStatus == OrderStatus.IN_DELIVERY;
    }

    /**
     * 택배사 표시명 반환
     * @return 택배사 표시명
     */
    public String getCourierDisplayName() {
        return courierCompany != null ? courierCompany.getDisplayName() : "알 수 없음";
    }
}