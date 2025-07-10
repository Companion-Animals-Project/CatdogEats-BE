package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.CourierCompany;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.request.OrderStatusUpdateRequest;
import com.team5.catdogeats.orders.dto.request.TrackingNumberRegisterRequest;
import com.team5.catdogeats.orders.dto.response.OrderStatusUpdateResponse;
import com.team5.catdogeats.orders.dto.response.ShipmentSyncResponse;
import com.team5.catdogeats.orders.dto.response.TrackingNumberRegisterResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.SellerOrderCommandService;
import com.team5.catdogeats.orders.service.ShipmentSyncService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * 판매자용 주문 쓰기 전용 서비스 구현체 (CQRS Command)
 * 단일 책임: 주문 변경 관련 기능만 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderCommandServiceImpl implements SellerOrderCommandService {

    private final UserRepository userRepository;
    private final SellersRepository sellerRepository;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentSyncService shipmentSyncService;

    /**
     * 주문 상태 변경 (배송 상태 관리)
     */
    @Override
    @JpaTransactional
    public OrderStatusUpdateResponse updateOrderStatus(UserPrincipal userPrincipal, OrderStatusUpdateRequest request) {
        try {
            log.debug("주문 상태 변경 시작 - provider={}, providerId={}, orderNumber={}, newStatus={}",
                    userPrincipal.provider(), userPrincipal.providerId(),
                    request.orderNumber(), request.newStatus());

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 확인
            Shipments shipment = findShipmentByOrderNumber(request.orderNumber());
            Orders order = shipment.getOrders();

            // 3. 판매자 소유 상품 확인
            validateSellerOwnership(seller, order);

            // 4. 상태 전환 유효성 검증
            validateStatusTransitionForStatusUpdate(order.getOrderStatus(), request.newStatus(), request);

            // 5. 상태 업데이트 및 추가 처리
            OrderStatus oldStatus = order.getOrderStatus();
            order.setOrderStatus(request.newStatus());

            // 6. 상태별 추가 처리
            handleStatusSpecificActions(shipment, request, oldStatus);

            // 7. 데이터 저장
            orderRepository.save(order);
            shipmentRepository.save(shipment);

            // 8. 응답 생성
            OrderStatusUpdateResponse response = buildStatusUpdateResponse(order, shipment, request, oldStatus);

            log.info("주문 상태 변경 완료 - orderNumber={}, oldStatus={}, newStatus={}",
                    request.orderNumber(), oldStatus, request.newStatus());

            return response;

        } catch (NoSuchElementException e) {
            log.warn("주문 상태 변경 실패 - orderNumber={}, reason={}",
                    request.orderNumber(), e.getMessage());
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("주문 상태 변경 검증 실패 - orderNumber={}, reason={}",
                    request.orderNumber(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 상태 변경 중 오류 - orderNumber={}", request.orderNumber(), e);
            throw new RuntimeException("주문 상태 변경 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 전체 배송 상태 동기화
     */
    @Override
    public ShipmentSyncResponse syncAllShipmentStatus(UserPrincipal userPrincipal) {
        return shipmentSyncService.syncAllShipmentStatus(userPrincipal);
    }

    /**
     * 특정 주문 배송 상태 동기화
     */
    @Override
    public ShipmentSyncResponse syncSingleShipmentStatus(UserPrincipal userPrincipal, String orderNumber) {
        return shipmentSyncService.syncSingleShipmentStatus(userPrincipal, orderNumber);
    }

    /**
     * 운송장 번호 등록 (판매자)
     */
    @Override
    @JpaTransactional
    public TrackingNumberRegisterResponse registerTrackingNumber(UserPrincipal userPrincipal, TrackingNumberRegisterRequest request) {
        try {
            log.debug("운송장 번호 등록 시작 - provider={}, providerId={}, orderNumber={}",
                    userPrincipal.provider(), userPrincipal.providerId(), request.orderNumber());

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 확인
            Shipments shipment = findShipmentByOrderNumber(request.orderNumber());
            Orders order = shipment.getOrders();

            // 3. 판매자 소유 상품 확인
            validateSellerOwnership(seller, order);

            // 4. 운송장 등록 가능 상태 확인
            validateTrackingRegistrationStatus(order);

            // 5. 운송장 번호 중복 확인
            validateTrackingNumberDuplication(request.courierCompany(), request.trackingNumber());

            // 6. 배송 정보 업데이트
            ZonedDateTime shippedAt = updateShipmentWithTracking(shipment, request);

            // 7. 주문 상태를 IN_DELIVERY로 변경 (요청 시)
            if (request.shouldStartShipment()) {
                order.setOrderStatus(OrderStatus.IN_DELIVERY);
                orderRepository.save(order);
            }

            // 8. 배송 정보 저장
            shipmentRepository.save(shipment);

            // 9. 응답 생성
            TrackingNumberRegisterResponse response = buildTrackingRegisterResponse(
                    order, shipment, request, shippedAt);

            log.info("운송장 번호 등록 완료 - orderNumber={}, trackingNumber={}, courier={}",
                    request.orderNumber(), request.trackingNumber(), request.courierCompany());

            return response;

        } catch (NoSuchElementException e) {
            log.warn("운송장 번호 등록 실패 - orderNumber={}, reason={}",
                    request.orderNumber(), e.getMessage());
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("운송장 번호 등록 검증 실패 - orderNumber={}, reason={}",
                    request.orderNumber(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("운송장 번호 등록 중 오류 - orderNumber={}", request.orderNumber(), e);
            throw new RuntimeException("운송장 번호 등록 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 주문 삭제 (목록 숨김 처리)
     */
    @Override
    @JpaTransactional
    public boolean deleteOrder(UserPrincipal userPrincipal, String orderNumber) {
        try {
            log.debug("주문 삭제 요청 - provider={}, providerId={}, orderNumber={}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 확인
            Shipments shipment = findShipmentByOrderNumber(orderNumber);
            Orders order = shipment.getOrders();

            // 3. 판매자 소유 상품 확인
            validateSellerOwnership(seller, order);

            // 4. 삭제 가능 상태 확인
            validateOrderDeletionStatus(order);

            // 5. 이미 숨김 처리된 주문인지 확인
            if (Boolean.TRUE.equals(order.getIsHidden())) {
                log.warn("이미 숨김 처리된 주문 - orderNumber={}", orderNumber);
                throw new IllegalArgumentException("이미 숨김 처리된 주문입니다");
            }

            // 6. 논리적 삭제 처리
            order.setIsHidden(true);
            order.setHiddenAt(ZonedDateTime.now());
            orderRepository.save(order);

            log.info("주문 삭제(숨김) 완료 - orderNumber={}", orderNumber);
            return true;

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("주문 삭제 실패 - orderNumber={}, reason={}", orderNumber, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 삭제 중 오류 - orderNumber={}", orderNumber, e);
            throw new RuntimeException("주문 삭제 중 오류가 발생했습니다", e);
        }
    }

    // ===== Private Helper Methods =====

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
     * 주문 조회
     */
    private Shipments findShipmentByOrderNumber(String orderNumber) {
        return shipmentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("주문을 찾을 수 없습니다: %s", orderNumber)));
    }

    /**
     * 판매자 소유 상품 확인
     */
    private void validateSellerOwnership(Sellers seller, Orders order) {
        boolean hasSellerProducts = order.getOrderItems().stream()
                .anyMatch(item -> item.getProducts().getSeller().getUserId().equals(seller.getUserId()));

        if (!hasSellerProducts) {
            throw new IllegalArgumentException("해당 주문에 대한 접근 권한이 없습니다");
        }
    }

    /**
     * 상태 전환 유효성 검증 (상태 변경 API용)
     */
    private void validateStatusTransitionForStatusUpdate(OrderStatus currentStatus, OrderStatus newStatus, OrderStatusUpdateRequest request) {
        // 허용되는 상태 전환 규칙 정의
        Set<OrderStatus> allowedTransitions = switch (currentStatus) {
            case PAYMENT_COMPLETED -> Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED);
            case PREPARING -> Set.of(OrderStatus.READY_FOR_SHIPMENT, OrderStatus.CANCELLED);
            case READY_FOR_SHIPMENT -> Set.of(OrderStatus.IN_DELIVERY, OrderStatus.CANCELLED);
            case IN_DELIVERY -> Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
            case DELIVERED -> Set.of(); // 배송 완료 후에는 상태 변경 불가
            case CANCELLED -> Set.of(); // 취소 후에는 상태 변경 불가
            case REFUNDED -> Set.of(); // 환불 후에는 상태 변경 불가
            default -> Set.of();
        };

        if (!allowedTransitions.contains(newStatus)) {
            throw new IllegalStateException(generateStatusTransitionErrorMessage(currentStatus, newStatus));
        }
    }

    /**
     * 상태 전환 에러 메시지 생성
     */
    private String generateStatusTransitionErrorMessage(OrderStatus currentStatus, OrderStatus newStatus) {
        return switch (newStatus) {
            case PREPARING -> "결제 완료 상태에서만 상품 준비 중으로 변경할 수 있습니다.";
            case READY_FOR_SHIPMENT -> "상품 준비 중 상태에서만 배송 준비 완료로 변경할 수 있습니다.";
            case IN_DELIVERY -> "배송 준비 완료 상태에서만 배송 중으로 변경할 수 있습니다.";
            case DELIVERED -> "배송 완료 상태는 시스템에서 자동으로 업데이트됩니다.";
            default -> {
                if (currentStatus == OrderStatus.DELIVERED) {
                    yield "배송 완료된 주문의 상태는 변경할 수 없습니다.";
                } else if (currentStatus == OrderStatus.CANCELLED) {
                    yield "취소된 주문의 상태는 변경할 수 없습니다.";
                } else {
                    yield String.format("허용되지 않는 상태 전환입니다: %s -> %s", currentStatus, newStatus);
                }
            }
        };
    }

    /**
     * 주문 삭제 가능 상태 확인
     */
    private void validateOrderDeletionStatus(Orders order) {
        Set<OrderStatus> deletableStatuses = Set.of(
                OrderStatus.DELIVERED,
                OrderStatus.CANCELLED,
                OrderStatus.REFUNDED
        );

        if (!deletableStatuses.contains(order.getOrderStatus())) {
            throw new IllegalArgumentException(
                    String.format("현재 주문 상태에서는 삭제할 수 없습니다: %s", order.getOrderStatus()));
        }
    }

    /**
     * 운송장 등록 가능 상태 확인
     */
    private void validateTrackingRegistrationStatus(Orders order) {
        Set<OrderStatus> allowedStatuses = Set.of(
                OrderStatus.READY_FOR_SHIPMENT
        );

        if (!allowedStatuses.contains(order.getOrderStatus())) {
            throw new IllegalStateException(
                    String.format("현재 주문 상태에서는 운송장 등록이 불가능합니다: %s", order.getOrderStatus()));
        }
    }

    /**
     * 운송장 번호 중복 확인
     */
    private void validateTrackingNumberDuplication(CourierCompany courier, String trackingNumber) {
        Optional<Shipments> existingShipment = shipmentRepository
                .findByCourierAndTrackingNumber(courier.getDisplayName(), trackingNumber);

        if (existingShipment.isPresent()) {
            throw new IllegalArgumentException(
                    String.format("이미 등록된 운송장 번호입니다: %s - %s", courier.getDisplayName(), trackingNumber));
        }
    }

    /**
     * 상태별 추가 처리
     */
    private void handleStatusSpecificActions(Shipments shipment, OrderStatusUpdateRequest request, OrderStatus oldStatus) {
        switch (request.newStatus()) {
            case DELIVERED -> {
                // 배송 완료 시 배송 완료 시간 설정
                shipment.setDeliveredAt(ZonedDateTime.now());
                shipment.setTrackingUpdatedAt(ZonedDateTime.now());
            }
            case IN_DELIVERY -> {
                // 배송 시작 시 배송 시작 시간 설정
                if (shipment.getShippedAt() == null) {
                    shipment.setShippedAt(ZonedDateTime.now());
                }
                shipment.setTrackingUpdatedAt(ZonedDateTime.now());
            }
            case CANCELLED -> // 취소 시 취소 시간 설정
                    shipment.setTrackingUpdatedAt(ZonedDateTime.now());
        }
    }

    /**
     * 배송 정보 업데이트 (운송장 등록)
     */
    private ZonedDateTime updateShipmentWithTracking(Shipments shipment, TrackingNumberRegisterRequest request) {
        ZonedDateTime shippedAt = ZonedDateTime.now();

        shipment.setCourier(request.courierCompany().getDisplayName());
        shipment.setTrackingNumber(request.trackingNumber());
        shipment.setShipmentMemo(request.shipmentMemo());
        shipment.setShippedAt(shippedAt);
        shipment.setTrackingUpdatedAt(shippedAt);

        return shippedAt;
    }

    /**
     * 상태 업데이트 응답 생성
     */
    private OrderStatusUpdateResponse buildStatusUpdateResponse(Orders order, Shipments shipment,
                                                                OrderStatusUpdateRequest request, OrderStatus oldStatus) {
        String message = switch (request.newStatus()) {
            case PREPARING -> "상품 준비 중 상태로 변경되었습니다.";
            case READY_FOR_SHIPMENT -> "배송 준비 완료 상태로 변경되었습니다.";
            case IN_DELIVERY -> "배송 중 상태로 변경되었습니다.";
            case DELIVERED -> "배송 완료 상태로 변경되었습니다.";
            case CANCELLED -> "주문이 취소되었습니다.";
            default -> "주문 상태가 변경되었습니다.";
        };

        // 지연 처리 여부 및 관련 정보 설정
        boolean isDelayed = request.isDelayed() != null ? request.isDelayed() : false;
        String delayReason = isDelayed ? request.reason() : null;
        ZonedDateTime expectedDeliveryDate = null;

        // 예상 배송일 파싱 (지연인 경우에만)
        if (isDelayed && request.expectedShipDate() != null) {
            try {
                expectedDeliveryDate = ZonedDateTime.parse(request.expectedShipDate() + "T00:00:00Z");
            } catch (Exception e) {
                log.warn("예상 배송일 파싱 실패: {}", request.expectedShipDate());
            }
        }

        return OrderStatusUpdateResponse.builder()
                .orderNumber(order.getOrderNumber())
                .previousStatus(oldStatus)  // oldStatus → previousStatus 수정
                .currentStatus(request.newStatus())  // newStatus → currentStatus 수정
                .reason(request.reason())
                .updatedAt(ZonedDateTime.now())
                .message(message)
                .isDelayed(isDelayed)
                .delayReason(delayReason)
                .expectedDeliveryDate(expectedDeliveryDate)
                .build();
    }

    /**
     * 운송장 등록 응답 생성
     */
    private TrackingNumberRegisterResponse buildTrackingRegisterResponse(Orders order, Shipments shipment,
                                                                         TrackingNumberRegisterRequest request, ZonedDateTime shippedAt) {
        String message = request.shouldStartShipment()
                ? "운송장 번호가 등록되고 배송이 시작되었습니다"
                : "운송장 번호가 등록되었습니다";

        // 배송 정보 생성
        TrackingNumberRegisterResponse.ShipmentInfo shipmentInfo =
                TrackingNumberRegisterResponse.ShipmentInfo.builder()
                        .courier(request.courierCompany().getDisplayName())
                        .trackingNumber(request.trackingNumber())
                        .shipmentMemo(request.shipmentMemo())
                        .shippedAt(shippedAt)
                        .build();

        return TrackingNumberRegisterResponse.builder()
                .orderNumber(order.getOrderNumber())
                .trackingNumber(request.trackingNumber())
                .courierCompany(request.courierCompany())
                .orderStatus(order.getOrderStatus())
                .shippedAt(shippedAt)
                .message(message)
                .shipmentInfo(shipmentInfo)
                .build();
    }
}