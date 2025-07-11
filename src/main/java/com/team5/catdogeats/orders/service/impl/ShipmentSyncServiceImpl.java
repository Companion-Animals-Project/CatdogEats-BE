package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.response.ShipmentSyncResponse;
import com.team5.catdogeats.orders.dto.response.TrackingResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.LogisticsTrackingService;
import com.team5.catdogeats.orders.service.ShipmentSyncService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 배송 상태 동기화 서비스 구현체
 * 테스트 물류 서버와 연동하여 배송 상태를 동기화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentSyncServiceImpl implements ShipmentSyncService {

    private final UserRepository userRepository;
    private final SellersRepository sellerRepository;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final LogisticsTrackingService logisticsTrackingService;

    /**
     * 전체 배송 상태 동기화 (판매자용)
     */
    @Override
    @JpaTransactional
    public ShipmentSyncResponse syncAllShipmentStatus(UserPrincipal userPrincipal) {
        try {
            log.info("배송 상태 동기화 시작 - provider: {}, providerId: {}",
                    userPrincipal.provider(), userPrincipal.providerId());

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 배송 중인 주문 목록 조회
            List<Shipments> inDeliveryShipments = findSellerInDeliveryShipments(seller);

            if (inDeliveryShipments.isEmpty()) {
                log.info("동기화할 배송 중인 주문이 없습니다 - sellerId: {}", seller.getUserId());
                return ShipmentSyncResponse.success(0, 0, 0, List.of(), List.of());
            }

            // 3. 각 주문의 배송 상태 확인 및 업데이트
            SyncResult syncResult = processBatchSync(inDeliveryShipments);

            // 4. 응답 DTO 생성
            return ShipmentSyncResponse.success(
                    inDeliveryShipments.size(),
                    syncResult.updatedOrderList().size(),
                    syncResult.failedOrderList().size(),
                    syncResult.updatedOrderList(),
                    syncResult.failedOrderList()
            );

        } catch (Exception e) {
            log.error("배송 상태 동기화 중 오류", e);
            throw new RuntimeException("배송 상태 동기화 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 단일 주문 배송 상태 자동 동기화 (구매자 조회용)
     */
    @Override
    @JpaTransactional
    public boolean syncSingleOrderDeliveryStatus(String orderNumber) {
        try {
            log.debug("단일 주문 배송 상태 동기화 시작 - orderNumber: {}", orderNumber);

            // 1. 주문번호로 배송 정보 조회
            Shipments shipment = shipmentRepository.findByOrderNumber(orderNumber)
                    .orElseThrow(() -> new IllegalArgumentException(
                            String.format("주문을 찾을 수 없습니다: %s", orderNumber)));

            // 2. 이미 배송완료 상태면 동기화 불필요
            if (OrderStatus.DELIVERED.equals(shipment.getOrders().getOrderStatus())) {
                log.debug("이미 배송완료 상태 - orderNumber: {}", orderNumber);
                return false;
            }

            // 3. 운송장 번호가 없으면 동기화 불가
            if (shipment.getTrackingNumber() == null || shipment.getTrackingNumber().trim().isEmpty()) {
                log.debug("운송장 번호 없음 - orderNumber: {}", orderNumber);
                return false;
            }

            // 4. 개별 배송 정보 동기화 수행 (기존 로직 재활용)
            boolean isUpdated = processShipmentSync(shipment);

            if (isUpdated) {
                log.info("자동 동기화 완료 - orderNumber: {}, 배송완료로 상태 변경", orderNumber);
            }

            return isUpdated;

        } catch (IllegalArgumentException e) {
            log.warn("단일 주문 동기화 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("단일 주문 동기화 중 오류 - orderNumber: {}", orderNumber, e);
            throw new RuntimeException("배송 상태 동기화 중 오류가 발생했습니다", e);
        }
    }
    /**
     * 판매자 조회
     */
    private Sellers findSellerByPrincipal(UserPrincipal userPrincipal) {
        Users user = userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));

        return sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("판매자 권한이 없습니다"));
    }

    /**
     * 판매자의 배송 중인 주문 목록 조회
     */
    private List<Shipments> findSellerInDeliveryShipments(Sellers seller) {
        List<Shipments> allShipments = shipmentRepository.findAll();

        return allShipments.stream()
                .filter(shipment -> OrderStatus.IN_DELIVERY.equals(shipment.getOrders().getOrderStatus()))
                .filter(shipment -> hasSellerProducts(shipment.getOrders(), seller))
                .toList();
    }

    /**
     * 주문에 판매자 상품이 포함되어 있는지 확인
     */
    private boolean hasSellerProducts(Orders order, Sellers seller) {
        return order.getOrderItems().stream()
                .anyMatch(item -> item.getProducts().getSeller().getUserId().equals(seller.getUserId()));
    }

    /**
     * 배치 동기화 처리
     */
    private SyncResult processBatchSync(List<Shipments> shipments) {
        List<ShipmentSyncResponse.UpdatedOrderInfo> updatedList = new ArrayList<>();
        List<ShipmentSyncResponse.FailedOrderInfo> failedList = new ArrayList<>();

        for (Shipments shipment : shipments) {
            try {
                if (processShipmentSync(shipment)) {
                    // 성공적으로 업데이트된 경우
                    updatedList.add(ShipmentSyncResponse.UpdatedOrderInfo.of(
                            shipment.getOrders().getOrderNumber(),
                            shipment.getTrackingNumber(),
                            shipment.getCourier(),
                            shipment.getDeliveredAt()
                    ));
                }
            } catch (Exception e) {
                // 실패한 경우
                failedList.add(ShipmentSyncResponse.FailedOrderInfo.of(
                        shipment.getOrders().getOrderNumber(),
                        shipment.getTrackingNumber(),
                        shipment.getCourier(),
                        e.getMessage()
                ));
                log.warn("주문 동기화 실패 - orderNumber: {}, trackingNumber: {}, error: {}",
                        shipment.getOrders().getOrderNumber(), shipment.getTrackingNumber(), e.getMessage());
            }
        }

        return new SyncResult(updatedList, failedList);
    }

    /**
     * 개별 배송 정보 동기화 처리 (기존 로직 유지)
     * @param shipment 동기화할 배송 정보
     * @return 업데이트 여부 (true: 업데이트됨, false: 변경사항 없음)
     */
    private boolean processShipmentSync(Shipments shipment) {
        String trackingNumber = shipment.getTrackingNumber();

        log.debug("배송 상태 확인 시작 - orderNumber: {}, trackingNumber: {}",
                shipment.getOrders().getOrderNumber(), trackingNumber);

        // 물류 서버에서 배송 상태 조회
        Optional<TrackingResponse> trackingInfo = logisticsTrackingService.getTrackingInfo(trackingNumber);

        if (trackingInfo.isEmpty()) {
            throw new RuntimeException("물류 서버에서 배송 정보를 조회할 수 없습니다");
        }

        TrackingResponse response = trackingInfo.get();

        // DELIVERED 상태인 경우에만 업데이트
        if ("DELIVERED".equals(response.currentStatus())) {
            ZonedDateTime deliveredAt = ZonedDateTime.now();

            // 주문 상태 업데이트
            Orders order = shipment.getOrders();
            order.setOrderStatus(OrderStatus.DELIVERED);

            // 배송 정보 업데이트
            shipment.setDeliveredAt(deliveredAt);
            shipment.setTrackingUpdatedAt(deliveredAt);

            // 데이터베이스 저장
            orderRepository.save(order);
            shipmentRepository.save(shipment);

            log.info("주문 배송완료 업데이트 완료 - orderNumber: {}, trackingNumber: {}",
                    order.getOrderNumber(), trackingNumber);

            return true; // 업데이트됨
        }

        log.debug("배송 상태 변경 없음 - orderNumber: {}, currentStatus: {}",
                shipment.getOrders().getOrderNumber(), response.currentStatus());

        return false; // 변경사항 없음
    }

    /**
     * 동기화 결과를 담는 내부 클래스
     */
    private record SyncResult(
            List<ShipmentSyncResponse.UpdatedOrderInfo> updatedOrderList,
            List<ShipmentSyncResponse.FailedOrderInfo> failedOrderList
    ) {}
}