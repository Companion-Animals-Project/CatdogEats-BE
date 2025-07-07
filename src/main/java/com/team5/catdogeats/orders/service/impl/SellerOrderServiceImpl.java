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
import com.team5.catdogeats.orders.service.SellerOrderCommandService;
import com.team5.catdogeats.orders.service.SellerOrderQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 판매자용 주문 관리 서비스 구현체 (CQRS Facade 패턴)
 * 기존 인터페이스를 유지하면서 내부적으로 CQRS 패턴으로 재구성된 서비스들을 조합
 *
 * CQRS 구조:
 * - SellerOrderQueryService: 읽기 전용 작업 (주문 조회 관련)
 * - SellerOrderCommandService: 쓰기 전용 작업 (주문 변경 관련)
 *
 * 기존 5개 서비스에서 2개 CQRS 서비스로 재구성:
 * [기존] SellerOrderQueryService + SellerOrderListService → [신규] SellerOrderQueryService (읽기)
 * [기존] SellerOrderStatusService + SellerOrderTrackingService + SellerOrderVisibilityService → [신규] SellerOrderCommandService (쓰기)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderServiceImpl implements SellerOrderService {

    // CQRS 패턴 적용: 읽기/쓰기 서비스 분리
    private final SellerOrderQueryService queryService;
    private final SellerOrderCommandService commandService;

    /**
     * 판매자용 주문 상세 조회 (배송지 정보 포함) - 읽기 작업
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 조회할 주문 번호
     * @return 판매자용 주문 상세 정보
     */
    @Override
    public SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, String orderNumber) {
        return queryService.getSellerOrderDetail(userPrincipal, orderNumber);
    }

    /**
     * 판매자용 주문 목록 조회 (페이징) - 읽기 작업
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param pageable 페이징 및 정렬 정보
     * @return 판매자용 주문 목록
     */
    @Override
    public SellerOrderListResponse getSellerOrders(UserPrincipal userPrincipal, Pageable pageable) {
        return queryService.getSellerOrders(userPrincipal, pageable);
    }

    /**
     * 판매자용 주문 목록 조회 - 상태 필터링 (페이징) - 읽기 작업
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderStatus 필터링할 주문 상태
     * @param pageable 페이징 및 정렬 정보
     * @return 상태별 필터링된 주문 목록
     */
    @Override
    public SellerOrderListResponse getSellerOrdersByStatus(UserPrincipal userPrincipal, OrderStatus orderStatus, Pageable pageable) {
        SellerOrderListResponse response = queryService.getSellerOrders(userPrincipal, pageable);
        return filterOrdersByStatus(response, orderStatus);
    }

    /**
     * 판매자용 주문 검색 (페이징) - 읽기 작업
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param searchType 검색 타입
     * @param searchKeyword 검색 키워드
     * @param pageable 페이징 및 정렬 정보
     * @return 검색 결과 주문 목록
     */
    @Override
    public SellerOrderListResponse searchSellerOrders(UserPrincipal userPrincipal, String searchType, String searchKeyword, Pageable pageable) {
        SellerOrderListResponse response = queryService.getSellerOrders(userPrincipal, pageable);
        return filterOrdersBySearch(response, searchType, searchKeyword);
    }

    /**
     * 주문 상태 변경 - 쓰기 작업 (위임)
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param request 주문 상태 변경 요청 정보
     * @return 상태 변경 처리 결과
     */
    @Override
    public OrderStatusUpdateResponse updateOrderStatus(UserPrincipal userPrincipal, OrderStatusUpdateRequest request) {
        return commandService.updateOrderStatus(userPrincipal, request);
    }

    /**
     * 운송장 번호 등록 - 쓰기 작업 (위임)
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param request 운송장 번호 등록 요청 정보
     * @return 운송장 등록 처리 결과
     */
    @Override
    public TrackingNumberRegisterResponse registerTrackingNumber(UserPrincipal userPrincipal, TrackingNumberRegisterRequest request) {
        return commandService.registerTrackingNumber(userPrincipal, request);
    }

    /**
     * 주문 목록에서 숨김 처리 - 쓰기 작업 (위임)
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 숨김 처리할 주문 번호
     * @return 숨김 처리 결과
     */
    @Override
    public boolean hideOrderFromList(UserPrincipal userPrincipal, String orderNumber) {
        return commandService.deleteOrder(userPrincipal, orderNumber);
    }

    // ===== Private Helper Methods =====

    /**
     * 상태별 주문 필터링 (임시 구현)
     * 향후 Repository에서 직접 처리하거나 프론트엔드에서 처리 권장
     */
    private SellerOrderListResponse filterOrdersByStatus(SellerOrderListResponse response, OrderStatus orderStatus) {
        if (orderStatus == null) {
            return response;
        }

        var filteredOrders = response.orders().stream()
                .filter(order -> order.orderStatus().equals(orderStatus))
                .toList();

        return SellerOrderListResponse.builder()
                .orders(filteredOrders)
                .currentPage(response.currentPage())
                .totalPages(response.totalPages())
                .totalElements((long) filteredOrders.size())
                .pageSize(response.pageSize())
                .hasNext(response.hasNext())
                .hasPrevious(response.hasPrevious())
                .searchType(response.searchType())
                .searchKeyword(response.searchKeyword())
                .filterStatus(orderStatus) // 필터 상태 설정
                .build();
    }

    /**
     * 검색어별 주문 필터링 (임시 구현)
     * 향후 Repository에서 직접 처리하거나 프론트엔드에서 처리 권장
     */
    private SellerOrderListResponse filterOrdersBySearch(SellerOrderListResponse response,
                                                         String searchType, String searchKeyword) {
        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            return response;
        }

        var filteredOrders = response.orders().stream()
                .filter(order -> matchesSearchCriteria(order, searchType, searchKeyword))
                .toList();

        return SellerOrderListResponse.builder()
                .orders(filteredOrders)
                .currentPage(response.currentPage())
                .totalPages(response.totalPages())
                .totalElements((long) filteredOrders.size())
                .pageSize(response.pageSize())
                .hasNext(response.hasNext())
                .hasPrevious(response.hasPrevious())
                .searchType(searchType) // 검색 타입 설정
                .searchKeyword(searchKeyword) // 검색 키워드 설정
                .filterStatus(response.filterStatus())
                .build();
    }

    /**
     * 검색 조건 일치 확인 (임시 구현) - recipientName 대신 buyerName 사용
     */
    private boolean matchesSearchCriteria(SellerOrderListResponse.SellerOrderSummary order,
                                          String searchType, String searchKeyword) {
        String keyword = searchKeyword.toLowerCase().trim();

        return switch (searchType) {
            case "orderNumber" -> order.orderNumber().toLowerCase().contains(keyword);
            case "recipientName" -> order.buyerName().toLowerCase().contains(keyword); // buyerName 사용
            default -> false;
        };
    }
}