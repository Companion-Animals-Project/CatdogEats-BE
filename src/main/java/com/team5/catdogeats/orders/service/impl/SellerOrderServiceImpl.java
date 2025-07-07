package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.request.OrderStatusUpdateRequest;
import com.team5.catdogeats.orders.dto.request.TrackingNumberRegisterRequest;
import com.team5.catdogeats.orders.dto.response.OrderStatusUpdateResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
import com.team5.catdogeats.orders.dto.response.TrackingNumberRegisterResponse;
import com.team5.catdogeats.orders.service.SellerOrderService;
import com.team5.catdogeats.orders.service.seller.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 판매자용 주문 관리 서비스 구현체 (Facade 패턴)
 * 기존 인터페이스를 유지하면서 내부적으로 기능별 서비스들을 조합
 *
 * 각 기능별 서비스:
 * - SellerOrderQueryService: 주문 상세 조회
 * - SellerOrderListService: 주문 목록 조회/검색/필터링
 * - SellerOrderStatusService: 주문 상태 변경
 * - SellerOrderTrackingService: 운송장 등록
 * - SellerOrderVisibilityService: 목록 숨김 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderServiceImpl implements SellerOrderService {

    // 기능별 서비스 주입
    private final SellerOrderQueryService queryService;
    private final SellerOrderListService listService;
    private final SellerOrderStatusService statusService;
    private final SellerOrderTrackingService trackingService;
    private final SellerOrderVisibilityService visibilityService;

    /**
     * 판매자용 주문 상세 조회 (배송지 정보 포함)
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 조회할 주문 번호
     * @return 판매자용 주문 상세 정보
     */
    @Override
    public SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, String orderNumber) {
        log.debug("판매자 주문 상세 조회 요청 위임 - orderNumber: {}", orderNumber);
        return queryService.getSellerOrderDetail(userPrincipal, orderNumber);
    }

    /**
     * 판매자용 주문 목록 조회 (페이징)
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param pageable 페이징 및 정렬 정보
     * @return 판매자용 주문 목록 (페이징)
     */
    @Override
    public SellerOrderListResponse getSellerOrders(UserPrincipal userPrincipal, Pageable pageable) {
        log.debug("판매자 주문 목록 조회 요청 위임 - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return listService.getSellerOrders(userPrincipal, pageable);
    }

    /**
     * 판매자용 주문 목록 조회 - 상태 필터링 (페이징)
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderStatus 필터링할 주문 상태
     * @param pageable 페이징 및 정렬 정보
     * @return 상태별 필터링된 주문 목록 (페이징)
     */
    @Override
    public SellerOrderListResponse getSellerOrdersByStatus(
            UserPrincipal userPrincipal,
            OrderStatus orderStatus,
            Pageable pageable) {

        log.debug("판매자 주문 목록 조회 (상태 필터링) 요청 위임 - status: {}", orderStatus);
        return listService.getSellerOrdersByStatus(userPrincipal, orderStatus, pageable);
    }

    /**
     * 판매자용 주문 검색 (페이징)
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param searchType 검색 타입
     * @param searchKeyword 검색 키워드
     * @param pageable 페이징 및 정렬 정보
     * @return 검색 결과 주문 목록 (페이징)
     */
    @Override
    public SellerOrderListResponse searchSellerOrders(
            UserPrincipal userPrincipal,
            String searchType,
            String searchKeyword,
            Pageable pageable) {

        log.debug("판매자 주문 검색 요청 위임 - searchType: {}, keyword: {}", searchType, searchKeyword);
        return listService.searchSellerOrders(userPrincipal, searchType, searchKeyword, pageable);
    }

    /**
     * 주문 상태 변경
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param request 주문 상태 변경 요청 정보
     * @return 상태 변경 처리 결과
     */
    @Override
    public OrderStatusUpdateResponse updateOrderStatus(UserPrincipal userPrincipal, OrderStatusUpdateRequest request) {
        log.debug("주문 상태 변경 요청 위임 - orderNumber: {}, newStatus: {}",
                request.orderNumber(), request.newStatus());
        return statusService.updateOrderStatus(userPrincipal, request);
    }

    /**
     * 운송장 번호 등록
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param request 운송장 번호 등록 요청 정보
     * @return 운송장 등록 처리 결과
     */
    @Override
    public TrackingNumberRegisterResponse registerTrackingNumber(
            UserPrincipal userPrincipal,
            TrackingNumberRegisterRequest request) {

        log.debug("운송장 번호 등록 요청 위임 - orderNumber: {}, courier: {}",
                request.orderNumber(), request.getCourierDisplayName());
        return trackingService.registerTrackingNumber(userPrincipal, request);
    }

    /**
     * 주문 목록에서 숨김 처리
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 숨김 처리할 주문 번호
     * @return 숨김 처리 결과
     */
    @Override
    public boolean hideOrderFromList(UserPrincipal userPrincipal, String orderNumber) {
        log.debug("주문 목록 숨김 처리 요청 위임 - orderNumber: {}", orderNumber);
        return visibilityService.hideOrderFromList(userPrincipal, orderNumber);
    }

    // ===== 추가 편의 메서드들 (기능별 서비스 직접 노출) =====

    /**
     * 주문 목록에서 숨김 해제 처리
     * 기존 인터페이스에는 없지만 유용한 기능이므로 추가
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 숨김 해제할 주문 번호
     * @return 숨김 해제 결과
     */
    public boolean showOrderInList(UserPrincipal userPrincipal, String orderNumber) {
        log.debug("주문 목록 숨김 해제 요청 - orderNumber: {}", orderNumber);
        return visibilityService.showOrderInList(userPrincipal, orderNumber);
    }

    /**
     * 주문 숨김 상태 조회
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 조회할 주문 번호
     * @return 숨김 상태
     */
    public boolean isOrderHidden(UserPrincipal userPrincipal, String orderNumber) {
        log.debug("주문 숨김 상태 조회 요청 - orderNumber: {}", orderNumber);
        return visibilityService.isOrderHidden(userPrincipal, orderNumber);
    }
}