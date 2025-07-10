package com.team5.catdogeats.orders.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 테스트 물류 서버 API와 통신하기 위한 Feign 클라이언트
 */
// name: Feign 클라이언트의 고유 이름, url: application.yml에 설정한 프로퍼티
@FeignClient(name = "logistics-api", url = "${logistics.api.url}")
public interface LogisticsClient {

    /**
     * 운송장 번호로 배송 상태 조회
     * @param trackingNumber 운송장 번호
     * @return TrackingResponse 배송 정보 응답
     */
    @GetMapping("/api/v1/trackings/{trackingNumber}")
    TrackingResponse getTrackingInfo(@PathVariable("trackingNumber") String trackingNumber);
}