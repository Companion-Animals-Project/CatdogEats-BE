package com.team5.catdogeats.orders.dto.response;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 물류 서버 API 응답 DTO
 * 테스트 물류 서버의 GET /api/v1/trackings/{trackingNumber} API 응답과 매칭
 */
public record TrackingResponse(
        String trackingNumber,                     // 운송장 번호
        String carrierCode,                        // 택배사 코드 (01, 04, 05, 06, 08)
        String currentStatus,                      // 현재 배송 상태 ("PICKED_UP", "AT_SORT_HUB", "DEPARTED_HUB", "OUT_FOR_DELIVERY", "DELIVERED")

        ZonedDateTime createdAt,                   // 운송장 생성 시간

        ZonedDateTime deliveredAt,                 // 배송 완료 시간 (null일 수 있음)

        List<TrackingLogResponse> logs             // 배송 추적 로그 목록
) {

    /**
     * 배송 완료 여부 확인
     * @return 배송 완료 여부
     */
    public boolean isDelivered() {
        return "DELIVERED".equals(currentStatus);
    }

    /**
     * 배송 중 여부 확인
     * @return 배송 중 여부 (픽업 완료부터 배송 완료 전까지)
     */
    public boolean isInTransit() {
        return currentStatus != null &&
                !"DELIVERED".equals(currentStatus) &&
                !"PICKED_UP".equals(currentStatus);
    }

    /**
     * 현재 상태의 한글 설명 반환
     * @return 배송 상태 한글 설명
     */
    public String getCurrentStatusDescription() {
        if (currentStatus == null) {
            return "알 수 없음";
        }

        return switch (currentStatus) {
            case "PICKED_UP" -> "물품 접수 완료";
            case "AT_SORT_HUB" -> "물류센터 도착";
            case "DEPARTED_HUB" -> "물류센터 출발";
            case "OUT_FOR_DELIVERY" -> "배송지 근처 도착";
            case "DELIVERED" -> "배송 완료";
            default -> currentStatus;
        };
    }

    /**
     * 배송 추적 로그 응답 DTO
     */
    public record TrackingLogResponse(
            String id,                               // 로그 ID
            String status,                         // 배송 상태
            String description,                    // 상태 설명

            ZonedDateTime timestamp                // 로그 생성 시간
    ) {

        /**
         * 상태의 한글 설명 반환
         * @return 배송 상태 한글 설명
         */
        public String getStatusDescription() {
            if (status == null) {
                return description != null ? description : "알 수 없음";
            }

            return switch (status) {
                case "PICKED_UP" -> "물품 접수 완료";
                case "AT_SORT_HUB" -> "물류센터 도착";
                case "DEPARTED_HUB" -> "물류센터 출발";
                case "OUT_FOR_DELIVERY" -> "배송지 근처 도착";
                case "DELIVERED" -> "배송 완료";
                default -> description != null ? description : status;
            };
        }
    }
}