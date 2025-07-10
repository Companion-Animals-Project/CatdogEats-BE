package com.team5.catdogeats.orders.dto.response;

/**
 * 물류 서버 API 응답 DTO
 */
public record TrackingResponse(
        String trackingNumber,
        String currentStatus // "DELIVERED", "IN_TRANSIT" 등의 상태 값
) {
}