package com.team5.catdogeats.orders.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 배송 상태 동기화 응답 DTO
 * API: POST /v1/sellers/orders/sync-shipment-status
 */
@Builder
public record ShipmentSyncResponse(
        int totalCheckedOrders,                    // 총 확인한 주문 수
        int updatedOrders,                         // 업데이트된 주문 수
        int failedOrders,                          // 실패한 주문 수
        List<UpdatedOrderInfo> updatedOrderList,  // 업데이트된 주문 목록
        List<FailedOrderInfo> failedOrderList,    // 실패한 주문 목록

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        ZonedDateTime syncedAt,                    // 동기화 수행 시간

        String message                             // 동기화 결과 메시지
) {

    /**
     * 업데이트된 주문 정보
     */
    @Builder
    public record UpdatedOrderInfo(
            String orderNumber,                    // 주문 번호
            String trackingNumber,                 // 운송장 번호
            String courier,                        // 택배사

            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
            ZonedDateTime deliveredAt              // 배송 완료 시간
    ) {
        /**
         * 업데이트된 주문 정보 생성 헬퍼 메서드
         */
        public static UpdatedOrderInfo of(String orderNumber, String trackingNumber,
                                          String courier, ZonedDateTime deliveredAt) {
            return UpdatedOrderInfo.builder()
                    .orderNumber(orderNumber)
                    .trackingNumber(trackingNumber)
                    .courier(courier)
                    .deliveredAt(deliveredAt)
                    .build();
        }
    }

    /**
     * 실패한 주문 정보
     */
    @Builder
    public record FailedOrderInfo(
            String orderNumber,                    // 주문 번호
            String trackingNumber,                 // 운송장 번호
            String courier,                        // 택배사
            String errorReason                     // 실패 사유
    ) {
        /**
         * 실패한 주문 정보 생성 헬퍼 메서드
         */
        public static FailedOrderInfo of(String orderNumber, String trackingNumber,
                                         String courier, String errorReason) {
            return FailedOrderInfo.builder()
                    .orderNumber(orderNumber)
                    .trackingNumber(trackingNumber)
                    .courier(courier)
                    .errorReason(errorReason)
                    .build();
        }
    }

    /**
     * 성공 응답 생성 헬퍼 메서드
     */
    public static ShipmentSyncResponse success(int totalChecked, int updated, int failed,
                                               List<UpdatedOrderInfo> updatedList,
                                               List<FailedOrderInfo> failedList) {
        String message;
        if (updated == 0) {
            message = "모든 주문이 이미 최신 상태입니다.";
        } else {
            message = String.format("%d개 주문이 배송완료로 업데이트되었습니다.", updated);
        }

        return ShipmentSyncResponse.builder()
                .totalCheckedOrders(totalChecked)
                .updatedOrders(updated)
                .failedOrders(failed)
                .updatedOrderList(updatedList)
                .failedOrderList(failedList)
                .syncedAt(ZonedDateTime.now())
                .message(message)
                .build();
    }

    /**
     * 오류 응답 생성 헬퍼 메서드
     */
    public static ShipmentSyncResponse error(String errorMessage) {
        return ShipmentSyncResponse.builder()
                .totalCheckedOrders(0)
                .updatedOrders(0)
                .failedOrders(0)
                .updatedOrderList(List.of())
                .failedOrderList(List.of())
                .syncedAt(ZonedDateTime.now())
                .message(errorMessage)
                .build();
    }

    /**
     * 동기화 결과 요약
     * @return 동기화 결과 요약 문자열
     */
    public String getSummary() {
        return String.format("총 %d개 주문 확인, %d개 업데이트, %d개 실패",
                totalCheckedOrders, updatedOrders, failedOrders);
    }
}