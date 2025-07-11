package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.response.BuyerOrderListResponse;
import com.team5.catdogeats.orders.dto.response.BuyerShipmentDetailResponse;
import com.team5.catdogeats.orders.dto.response.TrackingResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.BuyerOrderQueryService;
import com.team5.catdogeats.orders.service.LogisticsTrackingService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 구매자용 주문/배송 조회 전용 서비스 구현체
 * 단일 책임: 구매자의 주문 및 배송 정보 조회만 담당
 * CQRS 패턴 적용으로 기존 OrderService와 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuyerOrderQueryServiceImpl implements BuyerOrderQueryService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final LogisticsTrackingService logisticsTrackingService;

    /**
     * 구매자 주문 목록 조회 (배송 정보 포함)
     */
    @Override
    @JpaTransactional(readOnly = true)
    public BuyerOrderListResponse getBuyerOrderList(UserPrincipal userPrincipal, Pageable pageable) {
        log.info("구매자 주문 목록 조회 시작 - provider: {}, providerId: {}, page: {}, size: {}",
                userPrincipal.provider(), userPrincipal.providerId(),
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            // 1. 사용자 인증 및 조회
            Users user = findUserByPrincipal(userPrincipal);

            // 2. 페이징 정보 검증
            validatePageable(pageable);

            // 3. 구매자 주문 목록 조회 (연관 데이터 포함)
            Page<Orders> orderPage = orderRepository.findBuyerOrdersWithDetails(user, pageable);

            // 4. DTO 변환
            List<BuyerOrderListResponse.BuyerOrderSummary> orderSummaries =
                    orderPage.getContent().stream()
                            .map(this::convertToBuyerOrderSummary)
                            .toList();

            // 5. 응답 DTO 생성
            BuyerOrderListResponse response = BuyerOrderListResponse.of(
                    orderSummaries,
                    orderPage.getNumber(),
                    orderPage.getTotalPages(),
                    orderPage.getTotalElements(),
                    orderPage.getSize(),
                    orderPage.hasNext(),
                    orderPage.hasPrevious()
            );

            log.info("구매자 주문 목록 조회 성공 - page={}, size={}, totalElements={}",
                    pageable.getPageNumber(), pageable.getPageSize(), orderPage.getTotalElements());

            return response;

        } catch (IllegalArgumentException e) {
            log.warn("구매자 주문 목록 조회 인자 오류 - reason={}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("구매자 주문 목록 조회 중 오류 - provider={}, providerId={}",
                    userPrincipal.provider(), userPrincipal.providerId(), e);
            throw new RuntimeException("주문 목록 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 구매자 배송 정보 상세 조회 (물류 서버 연동)
     */
    @Override
    @JpaTransactional(readOnly = true)
    public BuyerShipmentDetailResponse getBuyerShipmentDetail(UserPrincipal userPrincipal, String orderNumber) {
        log.info("구매자 배송 정보 상세 조회 시작 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        try {
            // 1. 사용자 인증 및 조회
            Users user = findUserByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 확인
            Orders order = orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다."));

            // 3. 배송 정보 조회
            Shipments shipment = shipmentRepository.findByOrders(order)
                    .orElseThrow(() -> new IllegalStateException("배송 정보가 없는 주문입니다."));

            // 4. 물류 서버에서 실시간 배송 추적 로그 조회
            List<BuyerShipmentDetailResponse.TrackingLog> trackingLogs = getTrackingLogsFromLogisticsServer(shipment.getTrackingNumber());

            // 5. 운송장 정보 생성
            BuyerShipmentDetailResponse.TrackingInfo trackingInfo =
                    BuyerShipmentDetailResponse.TrackingInfo.of(
                            shipment.getTrackingNumber(),
                            shipment.getCourier()
                    );

            // 6. 수취인 정보 생성
            BuyerShipmentDetailResponse.RecipientInfo recipientInfo =
                    BuyerShipmentDetailResponse.RecipientInfo.of(
                            shipment.getRecipientName(),
                            shipment.getRecipientPhone(),
                            shipment.getPostalCode(),
                            shipment.getStreetAddress(),
                            shipment.getDetailAddress(),
                            shipment.getDeliveryRequest()
                    );

            // 7. 배송 상태에 따른 응답 분기
            BuyerShipmentDetailResponse response;
            if (shipment.getDeliveredAt() != null) {
                // 배송 완료된 경우 - 도착일 표시
                response = BuyerShipmentDetailResponse.withArrivalDate(
                        orderNumber,
                        shipment.getDeliveredAt(),
                        trackingInfo,
                        recipientInfo,
                        trackingLogs
                );
            } else {
                // 배송 중인 경우 - 현재 배송 상태 표시
                response = BuyerShipmentDetailResponse.withDeliveryStatus(
                        orderNumber,
                        order.getOrderStatus(),
                        trackingInfo,
                        recipientInfo,
                        trackingLogs
                );
            }

            log.info("구매자 배송 정보 상세 조회 성공 - orderNumber={}, status={}, logsCount={}",
                    orderNumber, order.getOrderStatus(), trackingLogs.size());

            return response;

        } catch (NoSuchElementException e) {
            log.warn("구매자 배송 정보 상세 조회 실패 - orderNumber={}, reason={}", orderNumber, e.getMessage());
            throw e;
        } catch (IllegalStateException e) {
            log.warn("구매자 배송 정보 상세 조회 상태 오류 - orderNumber={}, reason={}", orderNumber, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("구매자 배송 정보 상세 조회 중 오류 - orderNumber={}", orderNumber, e);
            throw new RuntimeException("배송 정보 상세 조회 중 오류가 발생했습니다", e);
        }
    }

    // ===== Private Helper Methods =====

    /**
     * 사용자 조회 - 기존 패턴 활용
     */
    private Users findUserByPrincipal(UserPrincipal userPrincipal) {
        return userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));
    }

    /**
     * 구매자용 주문 요약 정보 변환
     */
    private BuyerOrderListResponse.BuyerOrderSummary convertToBuyerOrderSummary(Orders order) {
        // 주문 상품 정보 요약 생성 ("상품명 외 N건")
        String orderItemsInfo = buildOrderItemsInfo(order.getOrderItems());

        // 배송 상태에 따른 분기 처리
        if (order.getShipment() != null && order.getShipment().getDeliveredAt() != null) {
            // 배송 완료된 경우 - 도착일 표시
            return BuyerOrderListResponse.BuyerOrderSummary.withArrivalDate(
                    order.getOrderNumber(),
                    order.getCreatedAt(),
                    order.getShipment().getDeliveredAt(),
                    orderItemsInfo,
                    order.getTotalPrice()
            );
        } else {
            // 배송 중인 경우 - 배송 상태 표시
            return BuyerOrderListResponse.BuyerOrderSummary.withDeliveryStatus(
                    order.getOrderNumber(),
                    order.getCreatedAt(),
                    order.getOrderStatus(),
                    orderItemsInfo,
                    order.getTotalPrice()
            );
        }
    }

    /**
     * 주문 상품 정보 요약 생성 ("상품명 외 N건")
     */
    private String buildOrderItemsInfo(List<OrderItems> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return "상품 정보 없음";
        }

        String firstProductName = orderItems.get(0).getProducts().getTitle();
        int additionalCount = orderItems.size() - 1;

        if (additionalCount > 0) {
            return firstProductName + " 외 " + additionalCount + "건";
        } else {
            return firstProductName;
        }
    }

    /**
     * 물류 서버에서 배송 추적 로그 조회
     */
    private List<BuyerShipmentDetailResponse.TrackingLog> getTrackingLogsFromLogisticsServer(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            log.warn("운송장 번호가 없어 배송 추적 로그를 조회할 수 없습니다");
            return List.of();
        }

        try {
            return logisticsTrackingService.getTrackingInfo(trackingNumber)
                    .map(this::convertTrackingLogs)
                    .orElse(List.of());
        } catch (Exception e) {
            log.warn("물류 서버 배송 추적 로그 조회 실패 - trackingNumber: {}, error: {}",
                    trackingNumber, e.getMessage());
            return List.of();
        }
    }

    /**
     * 물류 서버 응답을 BuyerShipmentDetailResponse.TrackingLog로 변환
     */
    private List<BuyerShipmentDetailResponse.TrackingLog> convertTrackingLogs(TrackingResponse trackingResponse) {
        return trackingResponse.logs().stream()
                .map(log -> BuyerShipmentDetailResponse.TrackingLog.of(
                        log.timestamp(),
                        log.status(),
                        "", // 물류 서버 응답에 location 정보가 없으므로 빈 문자열
                        log.description()
                ))
                .toList();
    }

    /**
     * 페이징 정보 검증
     */
    private void validatePageable(Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("페이지 크기는 100을 초과할 수 없습니다");
        }
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다");
        }
    }
}