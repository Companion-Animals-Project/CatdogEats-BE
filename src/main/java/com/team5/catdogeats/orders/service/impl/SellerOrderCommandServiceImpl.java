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
import com.team5.catdogeats.orders.dto.response.TrackingNumberRegisterResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.DeliveryTrackingService;
import com.team5.catdogeats.orders.service.SellerOrderCommandService;
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
    private final DeliveryTrackingService deliveryTrackingService;

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
            validateStatusTransition(order.getOrderStatus(), request.newStatus(), request);

            // 5. 상태 업데이트 및 추가 처리
            OrderStatus oldStatus = order.getOrderStatus();
            order.setOrderStatus(request.newStatus());

            // 6. 상태별 추가 처리
            handleStatusSpecificActions(shipment, request, oldStatus);

            // 7. 엔티티 저장
            orderRepository.save(order);
            shipmentRepository.save(shipment);

            // 8. 응답 생성
            OrderStatusUpdateResponse response = buildStatusUpdateResponse(order, shipment, request, oldStatus);

            log.info("주문 상태 변경 완료 - orderNumber={}, {} -> {}",
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
     * 운송장 번호 등록
     */
    @Override
    @JpaTransactional
    public TrackingNumberRegisterResponse registerTrackingNumber(UserPrincipal userPrincipal, TrackingNumberRegisterRequest request) {
        try {
            log.debug("운송장 번호 등록 시작 - provider={}, providerId={}, orderNumber={}, courier={}",
                    userPrincipal.provider(), userPrincipal.providerId(),
                    request.orderNumber(), request.courierCompany());

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

            // 6. 스마트택배 API 검증 (선택)
            TrackingNumberRegisterResponse.ValidationResult validationResult =
                    validateWithSmartDeliveryApi(request);

            // 7. 배송 정보 업데이트
            ZonedDateTime shippedAt = updateShipmentWithTracking(shipment, request);

            // 8. 자동으로 배송중 상태로 변경 (요청 시)
            if (request.shouldStartShipmentImmediately()) {
                order.setOrderStatus(OrderStatus.IN_DELIVERY);
                orderRepository.save(order);
            }

            // 9. 응답 DTO 생성
            TrackingNumberRegisterResponse response = buildTrackingRegisterResponse(
                    order, shipment, request, validationResult, shippedAt);

            log.info("운송장 번호 등록 성공 - orderNumber={}, trackingNumber={}, courier={}",
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
     * 주문 삭제 (숨김 처리) - 새로 구현된 메서드
     */
    @Override
    @JpaTransactional
    public void deleteOrder(UserPrincipal userPrincipal, String orderNumber) {
        try {
            log.debug("주문 삭제 요청 - provider={}, providerId={}, orderNumber={}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 확인
            Shipments shipment = findShipmentByOrderNumber(orderNumber);

            // 3. 판매자 소유 상품 확인
            validateSellerOwnership(seller, shipment.getOrders());

            // 4. 배송 정보 숨김 처리
            shipment.setIsHiddenBySeller(true);
            shipment.setHiddenAt(ZonedDateTime.now());
            shipmentRepository.save(shipment);

            log.info("주문 삭제 완료 - orderNumber={}", orderNumber);

        } catch (NoSuchElementException e) {
            log.warn("주문 삭제 실패 - orderNumber={}, reason={}", orderNumber, e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("주문 삭제 권한 오류 - orderNumber={}, reason={}", orderNumber, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 삭제 중 오류 - orderNumber={}", orderNumber, e);
            throw new RuntimeException("주문 삭제 중 오류가 발생했습니다", e);
        }
    }

    // ===== Private Helper Methods =====

    /**
     * 판매자 조회 - 기존 메서드 활용
     */
    private Sellers findSellerByPrincipal(UserPrincipal userPrincipal) {
        Users user = userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));

        return sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("판매자 권한이 없습니다"));
    }

    /**
     * 주문번호로 배송정보 조회 - 기존 메서드 활용
     */
    private Shipments findShipmentByOrderNumber(String orderNumber) {
        return shipmentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderNumber));
    }

    /**
     * 판매자 소유 상품 확인 - 기존 메서드명 활용
     */
    private void validateSellerOwnership(Sellers seller, Orders order) {
        boolean hasSellerProducts = order.getOrderItems().stream()
                .anyMatch(item -> item.getProducts().getSeller().getUserId().equals(seller.getUserId()));

        if (!hasSellerProducts) {
            throw new IllegalArgumentException("해당 주문에 대한 접근 권한이 없습니다");
        }
    }

    /**
     * 상태 전환 유효성 검증
     */
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus, OrderStatusUpdateRequest request) {
        // 허용되는 상태 전환 규칙 정의
        Set<OrderStatus> allowedTransitions = switch (currentStatus) {
            case PAYMENT_COMPLETED -> Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED);
            case PREPARING -> Set.of(OrderStatus.IN_DELIVERY, OrderStatus.CANCELLED);
            case IN_DELIVERY -> Set.of(OrderStatus.DELIVERED, OrderStatus.DELIVERY_DELAYED);
            case DELIVERY_DELAYED -> Set.of(OrderStatus.IN_DELIVERY, OrderStatus.DELIVERED);
            case DELIVERED -> Set.of(); // 배송 완료 후에는 상태 변경 불가
            case CANCELLED -> Set.of(); // 취소된 주문은 상태 변경 불가
            default -> Set.of();
        };

        if (!allowedTransitions.contains(newStatus)) {
            throw new IllegalStateException(
                    String.format("상태 전환이 불가능합니다: %s -> %s", currentStatus, newStatus));
        }

        // 배송 지연 상태로 변경 시 지연 사유 확인 (reason 필드 활용)
        if (newStatus == OrderStatus.DELIVERY_DELAYED &&
                (request.reason() == null || request.reason().trim().isEmpty())) {
            throw new IllegalArgumentException("배송 지연 상태로 변경할 때는 지연 사유가 필요합니다");
        }
    }

    /**
     * 운송장 등록 가능 상태 확인
     */
    private void validateTrackingRegistrationStatus(Orders order) {
        Set<OrderStatus> allowedStatuses = Set.of(
                OrderStatus.PAYMENT_COMPLETED,
                OrderStatus.PREPARING
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
     * 스마트택배 API 검증
     */
    private TrackingNumberRegisterResponse.ValidationResult validateWithSmartDeliveryApi(TrackingNumberRegisterRequest request) {
        if (!request.enableApiValidation()) {
            return TrackingNumberRegisterResponse.ValidationResult.skipped("API 검증을 수행하지 않았습니다");
        }

        try {
            // DeliveryTrackingService를 통한 API 검증
            boolean isValid = deliveryTrackingService.validateTrackingNumber(
                    request.getCourierApiCode(),
                    request.trackingNumber()
            );

            return isValid
                    ? TrackingNumberRegisterResponse.ValidationResult.success("운송장 번호가 유효합니다")
                    : TrackingNumberRegisterResponse.ValidationResult.failed("운송장 번호가 유효하지 않습니다");

        } catch (Exception e) {
            log.warn("스마트택배 API 검증 중 오류 - trackingNumber: {}, error: {}",
                    request.trackingNumber(), e.getMessage());
            return TrackingNumberRegisterResponse.ValidationResult.failed("API 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 상태별 추가 처리 - 존재하지 않는 필드 제거
     */
    private void handleStatusSpecificActions(Shipments shipment, OrderStatusUpdateRequest request, OrderStatus oldStatus) {
        switch (request.newStatus()) {
            case DELIVERY_DELAYED -> {
                // 배송 지연 시 shipmentMemo에 지연 사유 저장
                if (request.reason() != null) {
                    shipment.setShipmentMemo(request.reason());
                }
            }
            case DELIVERED -> {
                // 배송 완료 시 배송 완료 시간 설정
                shipment.setDeliveredAt(ZonedDateTime.now());
                shipment.setTrackingUpdatedAt(ZonedDateTime.now());
            }
            case IN_DELIVERY -> {
                // 배송 중으로 변경 시 배송 시작 시간 설정 (아직 설정되지 않은 경우)
                if (shipment.getShippedAt() == null) {
                    shipment.setShippedAt(ZonedDateTime.now());
                }
            }
        }
    }

    /**
     * 배송 정보 업데이트 - 기존 setter 메서드 활용
     */
    private ZonedDateTime updateShipmentWithTracking(Shipments shipment, TrackingNumberRegisterRequest request) {
        ZonedDateTime shippedAt = ZonedDateTime.now();

        shipment.setCourier(request.courierCompany().getDisplayName());
        shipment.setTrackingNumber(request.trackingNumber());
        shipment.setShippedAt(shippedAt);
        shipment.setTrackingUpdatedAt(shippedAt);

        if (request.shipmentMemo() != null && !request.shipmentMemo().trim().isEmpty()) {
            shipment.setShipmentMemo(request.shipmentMemo());
        }

        return shippedAt;
    }

    /**
     * 상태 업데이트 응답 생성
     */
    private OrderStatusUpdateResponse buildStatusUpdateResponse(Orders order, Shipments shipment,
                                                                OrderStatusUpdateRequest request, OrderStatus oldStatus) {
        return OrderStatusUpdateResponse.builder()
                .orderNumber(order.getOrderNumber())
                .previousStatus(oldStatus)
                .currentStatus(request.newStatus())
                .updatedAt(ZonedDateTime.now())
                .isDelayed(request.newStatus() == OrderStatus.DELIVERY_DELAYED)
                .delayReason(shipment.getShipmentMemo()) // shipmentMemo 활용
                .message(String.format("주문 상태가 %s에서 %s(으)로 변경되었습니다", oldStatus, request.newStatus()))
                .build();
    }

    /**
     * 운송장 등록 응답 생성
     */
    private TrackingNumberRegisterResponse buildTrackingRegisterResponse(Orders order, Shipments shipment,
                                                                         TrackingNumberRegisterRequest request,
                                                                         TrackingNumberRegisterResponse.ValidationResult validationResult,
                                                                         ZonedDateTime shippedAt) {
        return TrackingNumberRegisterResponse.successWithValidation(
                order.getOrderNumber(),
                request.trackingNumber(),
                request.courierCompany(),
                order.getOrderStatus(),
                shippedAt,
                validationResult
        );
    }
}