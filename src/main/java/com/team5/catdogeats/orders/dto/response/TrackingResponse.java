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
     * 배송 추적 로그 응답 DTO
     */
    public record TrackingLogResponse(
            String id,                               // 로그 ID
            String status,                         // 배송 상태
            String description,                    // 상태 설명

            ZonedDateTime timestamp                // 로그 생성 시간
    ) {}
}