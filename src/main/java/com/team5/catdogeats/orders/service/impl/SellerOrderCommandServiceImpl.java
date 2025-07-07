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
    private final DeliveryTrackingService deliveryTrackingService; // 추가: 실제 API 서비스

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

            // 5. 주문 상태 업데이트
            OrderStatus previousStatus = order.getOrderStatus();
            order.setOrderStatus(request.newStatus());
            orderRepository.save(order);

            // 6. 배송 정보 업데이트 (필요 시)
            updateShipmentForStatusChange(shipment, request);

            // 7. 응답 DTO 생성
            OrderStatusUpdateResponse response = buildStatusUpdateResponse(
                    order, shipment, previousStatus, request);

            log.info("주문 상태 변경 성공 - orderNumber={}, {} -> {}",
                    request.orderNumber(), previousStatus, request.newStatus());

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
     * 운송장 번호 등록 (배송 운송장 등록)
     */
    @Override
    @JpaTransactional
    public TrackingNumberRegisterResponse registerTrackingNumber(
            UserPrincipal userPrincipal, TrackingNumberRegisterRequest request) {
        try {
            log.debug("운송장 번호 등록 시작 - provider={}, providerId={}, orderNumber={}, courier={}",
                    userPrincipal.provider(), userPrincipal.providerId(),
                    request.orderNumber(), request.getCourierDisplayName());

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
            if (request.shouldStartShipment()) {
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

    // ===== Private Helper Methods =====

    /**
     * 판매자 조회
     */
    private Sellers findSellerByPrincipal(UserPrincipal userPrincipal) {
        Users user = userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));

        return sellerRepository.findByUsers(user)
                .orElseThrow(() -> new IllegalArgumentException("판매자 권한이 없습니다"));
    }

    /**
     * 주문번호로 배송정보 조회
     */
    private Shipments findShipmentByOrderNumber(String orderNumber) {
        return shipmentRepository.findByOrdersOrderNumber(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다"));
    }

    /**
     * 판매자 소유 상품 확인
     */
    private void validateSellerOwnership(Sellers seller, Orders order) {
        boolean hasSellerItem = order.getOrderItems().stream()
                .anyMatch(orderItem -> orderItem.getProducts().getSellers().equals(seller));

        if (!hasSellerItem) {
            throw new IllegalArgumentException("해당 주문에 대한 권한이 없습니다");
        }
    }

    /**
     * 상태 전환 유효성 검증
     */
    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus,
                                          OrderStatusUpdateRequest request) {
        // 허용된 상태 전환 규칙 검증
        boolean isValidTransition = switch (currentStatus) {
            case PAYMENT_COMPLETED -> newStatus == OrderStatus.PREPARING;
            case PREPARING -> newStatus == OrderStatus.READY_FOR_SHIPMENT ||
                    newStatus == OrderStatus.CANCELLED;
            case READY_FOR_SHIPMENT -> newStatus == OrderStatus.IN_DELIVERY ||
                    newStatus == OrderStatus.CANCELLED;
            default -> false;
        };

        if (!isValidTransition) {
            throw new IllegalArgumentException(
                    String.format("상태 전환이 불가능합니다: %s -> %s", currentStatus, newStatus));
        }
    }

    /**
     * 배송 정보 상태 변경 시 업데이트
     */
    private void updateShipmentForStatusChange(Shipments shipment, OrderStatusUpdateRequest request) {
        // 출고 지연 처리
        if (request.isDelayed() && request.expectedShipDate() != null) {
            shipment.setExpectedShipDate(request.expectedShipDate());
            shipment.setDelayReason(request.reason());
        }

        shipmentRepository.save(shipment);
    }

    /**
     * 운송장 등록 가능 상태 확인
     */
    private void validateTrackingRegistrationStatus(Orders order) {
        if (order.getOrderStatus() != OrderStatus.READY_FOR_SHIPMENT) {
            throw new IllegalStateException("배송준비완료 상태에서만 운송장을 등록할 수 있습니다");
        }
    }

    /**
     * 운송장 번호 중복 확인
     */
    private void validateTrackingNumberDuplication(CourierCompany courier, String trackingNumber) {
        boolean exists = shipmentRepository.existsByCourierAndTrackingNumber(
                courier.getApiCode(), trackingNumber);

        if (exists) {
            throw new IllegalArgumentException("이미 등록된 운송장 번호입니다");
        }
    }

    /**
     * 스마트택배 API 검증 (수정된 버전)
     */
    private TrackingNumberRegisterResponse.ValidationResult validateWithSmartDeliveryApi(
            TrackingNumberRegisterRequest request) {

        if (!request.shouldValidateWithApi()) {
            return TrackingNumberRegisterResponse.ValidationResult.skipped("API 검증을 건너뜀");
        }

        try {
            log.debug("스마트택배 API 검증 시작 - courierCode={}, trackingNumber={}",
                    request.getCourierApiCode(), request.trackingNumber());

            // DeliveryTrackingService를 통해 실제 API 검증 수행
            DeliveryTrackingService.ValidationResult result = deliveryTrackingService
                    .validateTrackingNumber(request.getCourierApiCode(), request.trackingNumber());

            // 결과 변환
            return switch (result.type()) {
                case "SUCCESS" -> TrackingNumberRegisterResponse.ValidationResult.success(result.message());
                case "INVALID" -> TrackingNumberRegisterResponse.ValidationResult.failed(result.message());
                case "ERROR" -> TrackingNumberRegisterResponse.ValidationResult.failed(result.message());
                case "SKIPPED" -> TrackingNumberRegisterResponse.ValidationResult.skipped(result.message());
                default -> TrackingNumberRegisterResponse.ValidationResult.failed("알 수 없는 검증 결과");
            };

        } catch (DeliveryTrackingService.DeliveryTrackingApiException e) {
            log.warn("스마트택배 API 검증 실패 - orderNumber={}, trackingNumber={}, reason={}",
                    request.orderNumber(), request.trackingNumber(), e.getMessage());

            // API 실패 시 기본 형식 검증으로 대체
            boolean isValidFormat = request.isValidTrackingNumberFormat();
            if (isValidFormat) {
                return TrackingNumberRegisterResponse.ValidationResult.success(
                        "기본 형식 검증 통과 (API 검증 실패)");
            } else {
                return TrackingNumberRegisterResponse.ValidationResult.failed(
                        "운송장 번호 형식이 올바르지 않습니다");
            }
        } catch (Exception e) {
            log.error("스마트택배 API 검증 중 예상치 못한 오류 - orderNumber={}, trackingNumber={}",
                    request.orderNumber(), request.trackingNumber(), e);

            // 예외 발생 시에도 기본 형식 검증으로 대체
            boolean isValidFormat = request.isValidTrackingNumberFormat();
            return isValidFormat
                    ? TrackingNumberRegisterResponse.ValidationResult.success("기본 형식 검증만 수행됨")
                    : TrackingNumberRegisterResponse.ValidationResult.failed("운송장 번호 형식이 올바르지 않습니다");
        }
    }

    /**
     * 배송 정보 운송장 업데이트
     */
    private ZonedDateTime updateShipmentWithTracking(Shipments shipment, TrackingNumberRegisterRequest request) {
        ZonedDateTime shippedAt = ZonedDateTime.now();

        shipment.setCourier(request.getCourierApiCode());
        shipment.setTrackingNumber(request.trackingNumber());
        shipment.setShippedAt(shippedAt);
        shipment.setTrackingUpdatedAt(shippedAt);

        if (request.shipmentMemo() != null && !request.shipmentMemo().trim().isEmpty()) {
            shipment.setDeliveryRequest(request.shipmentMemo());
        }

        shipmentRepository.save(shipment);
        return shippedAt;
    }

    /**
     * 상태 변경 응답 DTO 생성
     */
    private OrderStatusUpdateResponse buildStatusUpdateResponse(
            Orders order, Shipments shipment, OrderStatus previousStatus, OrderStatusUpdateRequest request) {

        return OrderStatusUpdateResponse.builder()
                .orderNumber(order.getOrderNumber())
                .previousStatus(previousStatus)
                .newStatus(order.getOrderStatus())
                .updatedAt(ZonedDateTime.now())
                .reason(request.reason())
                .isDelayed(request.isDelayed())
                .expectedShipDate(request.expectedShipDate())
                .message(generateStatusChangeMessage(previousStatus, order.getOrderStatus()))
                .build();
    }

    /**
     * 운송장 등록 응답 DTO 생성
     */
    private TrackingNumberRegisterResponse buildTrackingRegisterResponse(
            Orders order, Shipments shipment, TrackingNumberRegisterRequest request,
            TrackingNumberRegisterResponse.ValidationResult validationResult, ZonedDateTime shippedAt) {

        return TrackingNumberRegisterResponse.builder()
                .orderNumber(order.getOrderNumber())
                .trackingNumber(request.trackingNumber())
                .courierCompany(request.courierCompany())
                .orderStatus(order.getOrderStatus())
                .shippedAt(shippedAt)
                .validationResult(validationResult)
                .message("운송장 번호가 성공적으로 등록되었습니다")
                .build();
    }

    /**
     * 상태 변경 메시지 생성
     */
    private String generateStatusChangeMessage(OrderStatus previousStatus, OrderStatus newStatus) {
        return switch (newStatus) {
            case PREPARING -> "상품 준비가 시작되었습니다";
            case READY_FOR_SHIPMENT -> "배송 준비가 완료되었습니다";
            case IN_DELIVERY -> "배송이 시작되었습니다";
            case CANCELLED -> "주문이 취소되었습니다";
            default -> String.format("주문 상태가 %s에서 %s로 변경되었습니다", previousStatus, newStatus);
        };
    }
}