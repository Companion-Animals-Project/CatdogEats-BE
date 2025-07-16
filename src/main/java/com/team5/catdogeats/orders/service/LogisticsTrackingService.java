package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.orders.dto.response.TrackingResponse;

import java.util.Optional;

/**
 * 물류 서버 연동 서비스
 * 테스트 물류 서버와의 통신을 담당합니다.
 */
public interface LogisticsTrackingService {

    /**
     * 운송장 번호로 배송 상태 조회
     * 테스트 물류 서버의 GET /api/v1/trackings/{trackingNumber} API를 호출하여
     * 해당 운송장의 현재 배송 상태를 조회합니다.
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 정보 (조회 실패 시 Optional.empty())
     */
    Optional<TrackingResponse> getTrackingInfo(String trackingNumber);

    /**
     * 배송 완료 여부 확인
     * 운송장 번호로 배송 상태를 조회하여 배송 완료 여부를 확인합니다.
     *
     * @param trackingNumber 운송장 번호
     * @return 배송 완료 여부 (조회 실패 시 false)
     */
    boolean isDelivered(String trackingNumber);
}