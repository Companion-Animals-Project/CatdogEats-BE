package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;

import java.util.NoSuchElementException;

/**
 * 판매자용 주문 관리 서비스 인터페이스
 * 판매자가 본인이 판매한 상품의 배송지 정보를 조회할 수 있는 기능을 제공합니다.
 */
public interface SellerOrderService {

    /**
     * 판매자용 주문 상세 조회 (배송지 정보 포함)
     * 판매자가 본인이 판매한 상품이 포함된 주문의 배송지 정보를 조회합니다.
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 주문번호로 주문 조회 및 판매자 소유 상품 확인
     * 3. 배송지 정보 조회 (Shipments 엔티티에서)
     * 4. 해당 판매자의 상품만 필터링하여 반환
     * 5. 판매자에게 필요한 정보만 포함된 응답 DTO 생성
     * 보안 정책:
     * - 판매자는 본인이 판매한 상품이 포함된 주문만 조회 가능
     * - 구매자의 민감정보(결제정보 등)는 제외하고 배송에 필요한 정보만 제공
     * - 다른 판매자의 상품 정보는 접근 불가
     *
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 조회할 주문 번호
     * @return 판매자용 주문 상세 정보 (배송지 정보 + 해당 판매자 상품 목록)
     * @throws NoSuchElementException 주문이 존재하지 않거나 접근 권한이 없는 경우
     * @throws IllegalArgumentException 판매자 권한이 없는 경우
     */
    SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, Long orderNumber);
}