package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.request.OrderStatusUpdateRequest;
import com.team5.catdogeats.orders.dto.request.TrackingNumberRegisterRequest;
import com.team5.catdogeats.orders.dto.response.OrderStatusUpdateResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
import com.team5.catdogeats.orders.dto.response.TrackingNumberRegisterResponse;
import org.springframework.data.domain.Pageable;

import java.util.NoSuchElementException;

/**
 * 판매자용 주문 관리 서비스 인터페이스 (확장)
 * 판매자가 본인이 판매한 상품의 배송 관리를 할 수 있는 기능들을 제공합니다.
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
    SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, String orderNumber);

    /**
     * 판매자용 주문 목록 조회 (페이징)
     * 판매자가 본인이 판매한 상품이 포함된 주문 목록을 페이징으로 조회합니다.
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 페이징 정보 검증 및 정렬 조건 적용
     * 3. 판매자 소유 주문 목록 조회 (숨김 처리된 주문 제외)
     * 4. 각 주문별 판매자 상품만 필터링
     * 5. 민감정보 마스킹 처리 (전화번호 등)
     * 6. 응답 DTO 생성 및 반환
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

    /**
     * 판매자용 주문 목록 조회 - 상태 필터링 (페이징)
     * 특정 주문 상태의 주문들만 필터링하여 조회합니다.
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderStatus 필터링할 주문 상태
     * @param pageable 페이징 및 정렬 정보
     * @return 상태별 필터링된 주문 목록 (페이징)
     * @throws IllegalArgumentException 판매자 권한이 없는 경우, 유효하지 않은 상태인 경우
     * @throws NoSuchElementException 판매자를 찾을 수 없는 경우
     */
    SellerOrderListResponse getSellerOrdersByStatus(
            UserPrincipal userPrincipal,
            OrderStatus orderStatus,
            Pageable pageable
    );

    /**
     * 판매자용 주문 검색 (페이징)
     * 주문번호 또는 수령인명으로 검색합니다.
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param searchType 검색 타입 ("orderNumber" 또는 "recipientName")
     * @param searchKeyword 검색 키워드
     * @param pageable 페이징 및 정렬 정보
     * @return 검색 결과 주문 목록 (페이징)
     * @throws IllegalArgumentException 판매자 권한이 없는 경우, 유효하지 않은 검색 타입인 경우
     * @throws NoSuchElementException 판매자를 찾을 수 없는 경우
     */
    SellerOrderListResponse searchSellerOrders(
            UserPrincipal userPrincipal,
            String searchType,
            String searchKeyword,
            Pageable pageable
    );

    /**
     * 주문 상태 변경
     * 판매자가 본인이 판매한 상품이 포함된 주문의 상태를 변경합니다.
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 주문 존재 여부 및 판매자 소유 상품 확인
     * 3. 현재 상태에서 요청 상태로의 전환 가능성 검증
     * 4. 상태 전환 규칙 및 제약 조건 확인
     * 5. 주문 상태 업데이트 (트랜잭션)
     * 6. 필요 시 관련 엔티티 동시 업데이트 (Shipments 등)
     * 7. 응답 DTO 생성 및 반환
     * 상태 전환 규칙:
     * - PAYMENT_COMPLETED → PREPARING (상품준비중)
     * - PREPARING → READY_FOR_SHIPMENT (배송준비완료)
     * - PREPARING → CANCELLED (주문취소) - 특별한 경우
     * - READY_FOR_SHIPMENT → IN_DELIVERY (배송중) - 운송장 등록 필요
     * - 역순 전환 및 단계 건너뛰기 금지
     *
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param request 주문 상태 변경 요청 정보
     * @return 상태 변경 처리 결과
     * @throws NoSuchElementException 주문이 존재하지 않거나 접근 권한이 없는 경우
     * @throws IllegalArgumentException 판매자 권한이 없는 경우, 유효하지 않은 상태 전환인 경우
     * @throws IllegalStateException 이미 처리 중인 주문이거나 변경할 수 없는 상태인 경우
     */
    OrderStatusUpdateResponse updateOrderStatus(UserPrincipal userPrincipal, OrderStatusUpdateRequest request);

    /**
     * 운송장 번호 등록
     * 판매자가 택배사에서 발급받은 운송장 번호를 등록하여 배송을 시작합니다.
     * 처리 과정:
     * 1. UserPrincipal로 판매자 인증 및 권한 확인
     * 2. 주문 존재 여부 및 판매자 소유 상품 확인
     * 3. 주문 상태가 운송장 등록 가능한 상태인지 확인 (READY_FOR_SHIPMENT)
     * 4. 운송장 번호 중복 확인 (같은 택배사 내에서)
     * 5. 스마트택배 API를 통한 운송장 번호 유효성 검증 (선택)
     * 6. Shipments 엔티티에 택배사/운송장번호/발송일시 저장
     * 7. 주문 상태를 IN_DELIVERY로 자동 변경 (요청 시)
     * 8. 응답 DTO 생성 및 반환
     * 보안 정책:
     * - 판매자는 본인이 판매한 상품이 포함된 주문만 처리 가능
     * - 이미 등록된 운송장은 변경 불가 (별도 API 필요)
     * - 스마트택배 API 연동으로 유효성 검증
     *
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param request 운송장 번호 등록 요청 정보
     * @return 운송장 등록 처리 결과
     * @throws NoSuchElementException 주문이 존재하지 않거나 접근 권한이 없는 경우
     * @throws IllegalArgumentException 판매자 권한이 없는 경우, 유효하지 않은 운송장 번호인 경우
     * @throws IllegalStateException 운송장 등록이 불가능한 상태이거나 중복된 운송장인 경우
     */
    TrackingNumberRegisterResponse registerTrackingNumber(
            UserPrincipal userPrincipal,
            TrackingNumberRegisterRequest request
    );

    /**
     * 주문 목록에서 숨김 처리
     * 배송 완료 또는 취소된 주문을 판매자 관리 목록에서 숨김 처리합니다.
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 숨김 처리할 주문 번호
     * @return 숨김 처리 결과
     * @throws NoSuchElementException 주문이 존재하지 않거나 접근 권한이 없는 경우
     * @throws IllegalArgumentException 판매자 권한이 없는 경우, 숨김 처리가 불가능한 상태인 경우
     */
    boolean hideOrderFromList(UserPrincipal userPrincipal, String orderNumber);

    /**
     * 판매자 주문 통계 조회
     * 판매자의 주문 현황을 통계로 제공합니다.
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @return 주문 통계 정보 (전체/상태별 주문 수 등)
     * @throws IllegalArgumentException 판매자 권한이 없는 경우
     * @throws NoSuchElementException 판매자를 찾을 수 없는 경우
     */
    //SellerOrderStatsResponse getSellerOrderStats(UserPrincipal userPrincipal);
}