package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.response.OrderDetailResponse;

import java.util.NoSuchElementException;

public interface OrderDetailService {

    /**
     * 주문 상세 조회 (구매자)
     * UserPrincipal을 통해 인증된 사용자의 주문만 조회 가능하도록 보안 강화
     * 처리 과정:
     * 1. UserPrincipal로 사용자 인증 및 구매자 권한 확인
     * 2. 주문 존재 여부 및 소유권 검증
     * 3. 주문 정보, 상품 정보, 결제 정보 조회
     * 4. 사용자 기본 주소 정보 조회 (배송지 정보)
     * 5. 응답 DTO 생성 및 반환
     * @param userPrincipal JWT에서 추출된 인증된 사용자 정보
     * @param orderNumber 조회할 주문 번호
     * @return 주문 상세 정보
     * @throws NoSuchElementException 주문이 존재하지 않거나 본인 주문이 아닌 경우
     * @throws IllegalArgumentException 구매자 권한이 없는 경우
     */
    OrderDetailResponse getOrderDetail(UserPrincipal userPrincipal, String orderNumber);
}
