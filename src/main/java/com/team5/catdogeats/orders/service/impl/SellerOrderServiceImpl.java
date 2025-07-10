package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
import com.team5.catdogeats.orders.service.SellerOrderService;
import com.team5.catdogeats.orders.service.SellerOrderQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * 판매자용 주문 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderServiceImpl implements SellerOrderService {

    // CQRS 패턴 적용: 읽기/쓰기 서비스 분리
    private final SellerOrderQueryService queryService;

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
}