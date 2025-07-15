package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.response.ShipmentSyncResponse;

/**
 * 배송 상태 동기화 서비스
 * 테스트 물류 서버와 연동하여 배송 상태를 동기화합니다.
 */
public interface ShipmentSyncService {

    /**
     * 전체 배송 상태 동기화 (판매자용)
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
     * 단일 주문 배송 상태 자동 동기화 (구매자 조회용)
     * 구매자가 배송 상세를 조회할 때 물류서버에서 배송완료 상태를 확인하고,
     * 배송완료된 경우 자동으로 DB 상태를 업데이트합니다.
     * 처리 과정:
     * 1. 주문번호로 배송 정보 조회
     * 2. 물류서버에서 현재 배송 상태 확인
     * 3. 'DELIVERED' 상태이고 DB는 아직 배송중인 경우에만 업데이트
     * 4. 주문 상태를 DELIVERED로 변경
     * 5. 배송완료 시각(deliveredAt) 설정
     * 사용 목적:
     * - 구매자 배송 상세 조회 시 실시간 상태 반영
     * - 판매자가 수동 업데이트하지 않아도 자동으로 동기화
     * - 데이터 일관성 보장 (물류서버 vs DB)
     *
     * @param orderNumber 동기화할 주문 번호
     * @return 동기화 수행 여부 (true: 배송완료로 업데이트됨, false: 변경사항 없음)
     * @throws IllegalArgumentException 주문번호가 유효하지 않은 경우
     * @throws RuntimeException 물류서버 연동 실패 시
     */
    boolean syncSingleOrderDeliveryStatus(String orderNumber);
}