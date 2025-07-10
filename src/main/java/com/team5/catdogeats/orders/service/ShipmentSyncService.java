package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.response.ShipmentSyncResponse;

/**
 * 배송 상태 동기화 서비스
 * 테스트 물류 서버와 연동하여 배송 상태를 동기화합니다.
 */
public interface ShipmentSyncService {

    /**
     * 전체 배송 상태 동기화
     * 판매자의 모든 배송 중(IN_DELIVERY) 주문에 대해 물류 서버에서 상태를 조회하고,
     * DELIVERED 상태인 주문들을 자동으로 배송 완료로 업데이트합니다.
     * 프로세스:
     * 1. 판매자 권한 확인
     * 2. 판매자의 IN_DELIVERY 상태 주문 목록 조회
     * 3. 각 주문의 trackingNumber로 물류 서버 API 호출
     * 4. currentStatus가 'DELIVERED'인 주문들을 DB에서 DELIVERED로 업데이트
     * 5. 동기화 결과 반환
     *
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @return 동기화 결과 (총 조회된 주문 수, 업데이트된 주문 수, 업데이트된 주문 목록)
     */
    ShipmentSyncResponse syncAllShipmentStatus(UserPrincipal userPrincipal);

    /**
     * 특정 주문 배송 상태 동기화
     * 특정 주문 번호에 대해서만 배송 상태를 동기화합니다.
     *
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 동기화할 주문 번호
     * @return 동기화 결과
     */
    ShipmentSyncResponse syncSingleShipmentStatus(UserPrincipal userPrincipal, String orderNumber);
}