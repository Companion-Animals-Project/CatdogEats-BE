package com.team5.catdogeats.orders.external;

import com.team5.catdogeats.orders.dto.response.TrackingValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 스마트택배 API 클라이언트
 * OpenFeign을 사용하여 스마트택배 배송 추적 API와 연동
 *
 * API 제한사항:
 * - 프리티어: 동일 운송장 일 최대 10건 조회 제한
 * - 일일 전체 호출 제한: 1000건
 *
 * 지원 택배사:
 * - 우체국택배 (01), CJ대한통운 (04), 한진택배 (05)
 * - 로젠택배 (06), 롯데택배 (08)
 */
@FeignClient(
        name = "delivery-tracking-api",
        url = "${smart-courier.api.base-url}",
        configuration = DeliveryTrackingApiConfiguration.class
)
public interface DeliveryTrackingApiClient {
    /**
     * 운송장 번호 유효성 검증 API
     * 운송장 번호가 올바른 형식이고 실제 존재하는지 검증합니다.
     *
     * 주의: 이 API도 배송 조회와 동일한 엔드포인트를 사용하지만,
     * 검증 목적으로 사용되므로 별도 메서드로 분리했습니다.
     *
     * @param t_key API 키
     * @param t_code 택배사 코드
     * @param t_invoice 검증할 운송장 번호
     * @return 운송장 검증 결과
     */
    @GetMapping("/trackingInfo")
    TrackingValidationResponse validateTrackingNumber(
            @RequestParam("t_key") String t_key,
            @RequestParam("t_code") String t_code,
            @RequestParam("t_invoice") String t_invoice
    );
}