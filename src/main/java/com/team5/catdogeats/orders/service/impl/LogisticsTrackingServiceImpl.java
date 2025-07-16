package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.orders.client.LogisticsClient;
import com.team5.catdogeats.orders.dto.response.TrackingResponse;
import com.team5.catdogeats.orders.service.LogisticsTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 물류 서버 연동 서비스 구현체
 * 테스트 물류 서버와의 통신을 담당합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsTrackingServiceImpl implements LogisticsTrackingService {

    private final LogisticsClient logisticsClient;

    /**
     * 운송장 번호로 배송 상태 조회
     */
    @Override
    public Optional<TrackingResponse> getTrackingInfo(String trackingNumber) {
        try {
            log.debug("물류 서버 API 호출 시작 - trackingNumber: {}", trackingNumber);

            TrackingResponse response = logisticsClient.getTrackingInfo(trackingNumber);

            log.debug("물류 서버 API 호출 성공 - trackingNumber: {}, currentStatus: {}",
                    trackingNumber, response.currentStatus());

            return Optional.of(response);

        } catch (Exception e) {
            log.warn("물류 서버 API 호출 실패 - trackingNumber: {}, error: {}",
                    trackingNumber, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 배송 완료 여부 확인
     */
    @Override
    public boolean isDelivered(String trackingNumber) {
        return getTrackingInfo(trackingNumber)
                .map(response -> "DELIVERED".equals(response.currentStatus()))
                .orElse(false);
    }
}