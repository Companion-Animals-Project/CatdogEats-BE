package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.dto.response.OrderDeleteResponse;
import com.team5.catdogeats.orders.dto.response.OrderDetailResponse;

import java.util.NoSuchElementException;

/**
 * 주문 관리 서비스 인터페이스 (보안 개선 버전)
 * JWT 인증을 통한 사용자 식별로 보안성을 강화했습니다.
 */
public interface OrderService {

    /**
     * UserPrincipal을 사용한 안전한 주문 생성 (보안 개선 버전)
     * 처리 과정:
     * 1. UserPrincipal로 사용자 인증 및 조회
     * 2. 구매자 권한 검증
     * 3. 주문 정보 검증 (상품 존재 여부, 재고 확인)
     * 4. 재고 차감 (동시성 제어)
     * 5. 주문 엔티티 및 주문 아이템 생성 (PENDING 상태)
     * 6. 토스 페이먼츠를 위한 정보 생성
     * 7. 주문 응답 DTO 반환
     * 모든 작업이 하나의 트랜잭션에서 수행되어 재고 차감 실패 시 주문도 롤백됩니다.
     * @param userPrincipal JWT에서 추출된 인증된 사용자 정보
     * @param request 주문 생성 요청 정보
     * @return 생성된 주문 정보 (토스 페이먼츠 연동 정보 포함)
     * @throws IllegalArgumentException 상품이 존재하지 않거나 재고가 부족한 경우, 구매자 권한이 없는 경우
     * @throws IllegalStateException 재고 차감 실패 (동시성 충돌) 시
     * @throws NoSuchElementException 사용자를 찾을 수 없는 경우
     */
    OrderCreateResponse createOrderByUserPrincipal(UserPrincipal userPrincipal, OrderCreateRequest request);

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
    OrderDetailResponse getOrderDetail(UserPrincipal userPrincipal, Long orderNumber);

    /**
     * 주문 내역 삭제 (구매자) - 논리적 삭제 방식
     * UserPrincipal을 통해 인증된 사용자의 주문만 삭제 가능하도록 보안 강화
     * 처리 과정:
     * 1. UserPrincipal로 사용자 인증 및 구매자 권한 확인
     * 2. 주문 존재 여부 및 소유권 검증
     * 3. 주문의 삭제 가능 여부 확인 (이미 숨겨진 주문 체크)
     * 4. 논리적 삭제 처리 (is_hidden = true, hidden_at = 현재시각)
     * 5. 성공/실패 결과 반환
     * 특징:
     * - 물리적 삭제가 아닌 논리적 삭제로 데이터 무결성 보장
     * - 모든 상태의 주문 삭제 가능 (완료, 취소, 배송중 등 무관)
     * - 판매 통계, 정산 데이터 보존으로 비즈니스 연속성 확보
     *
     * @param userPrincipal JWT에서 추출된 인증된 사용자 정보
     * @param orderNumber 삭제할 주문 번호
     * @return 삭제 처리 결과
     * @throws NoSuchElementException 사용자를 찾을 수 없거나 주문이 존재하지 않는 경우
     * @throws IllegalArgumentException 구매자 권한이 없는 경우, 이미 삭제된 주문인 경우
     */
    OrderDeleteResponse deleteOrder(UserPrincipal userPrincipal, Long orderNumber);
}