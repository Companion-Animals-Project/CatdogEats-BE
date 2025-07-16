package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.response.BuyerOrderListResponse;
import com.team5.catdogeats.orders.dto.response.BuyerShipmentDetailResponse;
import org.springframework.data.domain.Pageable;

import java.util.NoSuchElementException;

/**
 * 구매자용 주문/배송 조회 전용 서비스 인터페이스
 * 단일 책임: 구매자의 주문 및 배송 정보 조회만 담당
 * CQRS 패턴 적용으로 기존 OrderService와 분리
 */
public interface BuyerOrderQueryService {

    /**
     * 구매자 주문 목록 조회 (배송 정보 포함)
     * 구매자가 자신의 모든 주문 내역을 목록 형태로 조회
     * 처리 과정:
     * 1. UserPrincipal로 사용자 인증 및 구매자 권한 확인
     * 2. 페이징 정보 검증
     * 3. 구매자의 주문 목록 조회 (숨김 처리된 주문 제외)
     * 4. 각 주문별 상품 정보 요약 생성 ("상품명 외 N건")
     * 5. 배송 상태에 따른 표시 정보 분기 처리
     * 6. 응답 DTO 생성 및 반환
     * 표시 데이터:
     * - 주문일 (orderDate)
     * - 배송 상태 (deliveryStatus) 또는 도착일 (arrivalDate)
     * - 주문 상품 정보 (orderItemsInfo): "상품명 외 N건"
     * - 총 결제 금액 (totalPrice)
     * @param userPrincipal JWT에서 추출된 인증된 사용자 정보
     * @param pageable 페이징 및 정렬 정보
     * @return 구매자용 주문 목록 (페이징)
     * @throws IllegalArgumentException 구매자 권한이 없는 경우, 잘못된 페이징 정보인 경우
     * @throws NoSuchElementException 사용자를 찾을 수 없는 경우
     */
    BuyerOrderListResponse getBuyerOrderList(UserPrincipal userPrincipal, Pageable pageable);

    /**
     * 구매자 배송 정보 상세 조회 (물류 서버 연동)
     * 구매자가 특정 주문의 상세 배송 정보 및 실시간 배송 추적 로그 조회
     * 처리 과정:
     * 1. UserPrincipal로 사용자 인증 및 구매자 권한 확인
     * 2. 주문 존재 여부 및 소유권 검증
     * 3. 배송 정보 조회 (Shipments 엔티티)
     * 4. 테스트 물류 서버 API 호출하여 실시간 배송 추적 로그 조회
     * 5. 배송 상태에 따른 표시 정보 분기 처리
     * 6. 응답 DTO 생성 및 반환
     * 표시 데이터:
     * - 배송 상태: 완료 시 도착일, 그 외에는 현재 배송 상태
     * - 운송장 정보 (trackingNumber, carrierName)
     * - 수취인 정보 (recipientInfo): 이름, 주소, 연락처
     * - 배송 추적 로그 (trackingLogs): 실시간 배송 현황 로그
     * @param userPrincipal JWT에서 추출된 인증된 사용자 정보
     * @param orderNumber 조회할 주문 번호
     * @return 구매자용 배송 정보 상세
     * @throws NoSuchElementException 주문이 존재하지 않거나 본인 주문이 아닌 경우
     * @throws IllegalArgumentException 구매자 권한이 없는 경우
     * @throws IllegalStateException 배송 정보가 없는 주문인 경우
     */
    BuyerShipmentDetailResponse getBuyerShipmentDetail(UserPrincipal userPrincipal, String orderNumber);
}