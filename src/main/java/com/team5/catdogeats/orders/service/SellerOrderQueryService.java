package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
import org.springframework.data.domain.Pageable;

import java.util.NoSuchElementException;

/**
 * 판매자용 주문 읽기 전용 서비스 인터페이스 (CQRS Query)
 * 단일 책임: 주문 조회 관련 기능만 담당
 *
 * 포함 기능:
 * - 주문 상세 조회 (배송지 정보 포함)
 * - 주문 목록 조회 (단순 페이징만, 필터링/검색은 프론트 처리)
 */
public interface SellerOrderQueryService {

    /**
     * 판매자용 주문 상세 조회 (배송지 정보 포함)
     * 판매자가 본인이 판매한 상품이 포함된 주문의 배송지 정보를 조회
     *
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 주문번호로 주문 조회 및 판매자 소유 상품 확인
     * 3. 배송지 정보 조회 (Shipments 엔티티에서)
     * 4. 해당 판매자의 상품만 필터링하여 반환
     * 5. 판매자에게 필요한 정보만 포함된 응답 DTO 생성
     *
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
    SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, String orderNumber);

    /**
     * 판매자용 주문 목록 조회 (단순 페이징)
     * 판매자가 본인이 판매한 상품이 포함된 주문 목록을 페이징으로 조회
     *
     * 주의사항:
     * - 복잡한 필터링/검색 기능은 제외 (프론트엔드에서 처리)
     * - 단순 페이징과 기본 정렬만 지원
     * - 성능 최적화를 위해 최소한의 조회만 수행
     *
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 페이징 정보 검증 및 정렬 조건 적용
     * 3. 판매자 소유 주문 목록 조회 (숨김 처리된 주문 제외)
     * 4. 각 주문별 판매자 상품만 필터링
     * 5. 민감정보 마스킹 처리 (전화번호 등)
     * 6. 응답 DTO 생성 및 반환
     *
     * 보안 정책:
     * - 판매자는 본인이 판매한 상품이 포함된 주문만 조회 가능
     * - 구매자 개인정보는 필요 최소한만 제공 (배송용)
     * - 다른 판매자의 상품 정보는 제외
     *
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param pageable 페이징 및 정렬 정보
     * @return 판매자용 주문 목록 (페이징)
     * @throws IllegalArgumentException 판매자 권한이 없는 경우, 잘못된 페이징 정보인 경우
     * @throws NoSuchElementException 판매자를 찾을 수 없는 경우
     */
    SellerOrderListResponse getSellerOrders(UserPrincipal userPrincipal, Pageable pageable);
}