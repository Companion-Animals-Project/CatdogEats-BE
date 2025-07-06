package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.request.OrderStatusUpdateRequest;
import com.team5.catdogeats.orders.dto.request.TrackingNumberRegisterRequest;
import com.team5.catdogeats.orders.dto.response.*;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.SellerOrderService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 판매자용 주문 관리 서비스 구현체 (확장)
 * 기존 배송지 조회 기능에 목록 관리, 상태 변경, 운송장 등록 기능을 추가
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderServiceImpl implements SellerOrderService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;

    @Override
    public SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, String orderNumber) {
        try {
            log.debug("판매자용 주문 상세 조회 시작 - provider: {}, providerId: {}, orderNumber: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

            // 1. UserPrincipal로 판매자 조회 및 검증
            Sellers seller = findSellerByPrincipal(userPrincipal);
            log.debug("판매자 조회 완료 - sellerId: {}", seller.getUserId());

            // 2. 주문 조회 및 권한 검증 (Repository의 최적화된 메서드 활용)
            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(orderNumber, seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            log.debug("배송정보 및 권한 검증 완료 - shipmentId: {}, orderId: {}",
                    shipment.getId(), shipment.getOrders().getId());

            // 3. 해당 판매자의 상품만 이미 필터링된 상태
            List<OrderItems> sellerOrderItems = shipment.getOrders().getOrderItems();

            // 4. DTO 변환 및 응답 생성
            return buildSellerOrderDetailResponse(shipment, sellerOrderItems, seller.getUserId());

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("판매자용 주문 상세 조회 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("판매자용 주문 상세 조회 중 예상치 못한 오류 발생 - orderNumber: {}", orderNumber, e);
            throw new RuntimeException("주문 상세 조회 중 서버 오류가 발생했습니다", e);
        }
    }

    @Override
    public SellerOrderListResponse getSellerOrders(UserPrincipal userPrincipal, Pageable pageable) {
        try {
            log.debug("판매자용 주문 목록 조회 시작 - provider: {}, providerId: {}, page: {}, size: {}",
                    userPrincipal.provider(), userPrincipal.providerId(),
                    pageable.getPageNumber(), pageable.getPageSize());

            // 1. UserPrincipal로 판매자 조회 및 검증
            Sellers seller = findSellerByPrincipal(userPrincipal);
            log.debug("판매자 조회 완료 - sellerId: {}", seller.getUserId());

            // 2. 페이징 정보 검증
            validatePageable(pageable);

            // 3. 판매자 소유 주문 목록 조회
            Page<Shipments> shipmentPage = shipmentRepository
                    .findSellerOrdersWithPaging(seller.getUserId(), pageable);

            log.debug("주문 목록 조회 완료 - totalElements: {}, currentPage: {}, totalPages: {}",
                    shipmentPage.getTotalElements(), shipmentPage.getNumber(), shipmentPage.getTotalPages());

            // 4. DTO 변환
            List<SellerOrderListResponse.SellerOrderSummary> orderSummaries =
                    shipmentPage.getContent().stream()
                            .map(this::convertToSellerOrderSummary)
                            .toList();

            // 5. 응답 생성 및 반환
            return SellerOrderListResponse.builder()
                    .orders(orderSummaries)
                    .currentPage(shipmentPage.getNumber())
                    .totalPages(shipmentPage.getTotalPages())
                    .totalElements(shipmentPage.getTotalElements())
                    .pageSize(shipmentPage.getSize())
                    .hasNext(shipmentPage.hasNext())
                    .hasPrevious(shipmentPage.hasPrevious())
                    .build();

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("판매자용 주문 목록 조회 실패 - reason: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("판매자용 주문 목록 조회 중 예상치 못한 오류 발생", e);
            throw new RuntimeException("주문 목록 조회 중 서버 오류가 발생했습니다", e);
        }
    }

    @Override
    @JpaTransactional
    public OrderStatusUpdateResponse updateOrderStatus(UserPrincipal userPrincipal, OrderStatusUpdateRequest request) {
        try {
            log.debug("주문 상태 변경 시작 - orderNumber: {}, newStatus: {}",
                    request.getOrderNumber(), request.getNewStatus());

            // 1. UserPrincipal로 판매자 조회 및 검증
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 검증
            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(request.getOrderNumber(), seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            Orders order = shipment.getOrders();
            OrderStatus currentStatus = order.getOrderStatus();

            // 3. 상태 전환 검증
            validateStatusTransition(request, currentStatus);
            validateReasonRequirement(request);

            // 4. 상태 변경
            order.setOrderStatus(request.getNewStatus());

            // 5. 특별한 상태 변경 처리
            handleSpecialStatusChange(request, shipment);

            // 6. 저장
            orderRepository.save(order);

            log.info("주문 상태 변경 완료 - orderNumber: {}, {} → {}",
                    request.getOrderNumber(), currentStatus, request.getNewStatus());

            // 7. 응답 생성
            return OrderStatusUpdateResponse.success(
                    request.getOrderNumber(),
                    currentStatus,
                    request.getNewStatus(),
                    request.getReason()
            );

        } catch (NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            log.warn("주문 상태 변경 실패 - orderNumber: {}, reason: {}",
                    request.getOrderNumber(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 상태 변경 중 예상치 못한 오류 발생 - orderNumber: {}",
                    request.getOrderNumber(), e);
            throw new RuntimeException("주문 상태 변경 중 서버 오류가 발생했습니다", e);
        }
    }

    @Override
    @JpaTransactional
    public TrackingNumberRegisterResponse registerTrackingNumber(
            UserPrincipal userPrincipal,
            TrackingNumberRegisterRequest request) {

        try {
            log.debug("운송장 번호 등록 시작 - orderNumber: {}, courier: {}, trackingNumber: {}",
                    request.getOrderNumber(), request.getCourierDisplayName(), request.getTrackingNumber());

            // 1. UserPrincipal로 판매자 조회 및 검증
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 검증
            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(request.getOrderNumber(), seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            Orders order = shipment.getOrders();

            // 3. 운송장 등록 가능한 상태인지 검증
            validateTrackingNumberRegistration(order, shipment, request);

            // 4. 운송장 번호 중복 확인
            validateTrackingNumberDuplication(request);

            // 5. 스마트택배 API 검증 (옵션)
            TrackingNumberRegisterResponse.ValidationResult validationResult =
                    validateTrackingNumberWithApi(request);

            // 6. 운송장 정보 저장
            String normalizedTrackingNumber = request.getNormalizedTrackingNumber();
            shipment.setTrackingInfo(
                    request.getCourierDisplayName(),
                    normalizedTrackingNumber
            );

            // 7. 즉시 배송 시작 처리
            if (request.shouldStartShipmentImmediately()) {
                order.setOrderStatus(OrderStatus.IN_DELIVERY);
            }

            // 8. 저장
            orderRepository.save(order);
            shipmentRepository.save(shipment);

            log.info("운송장 번호 등록 완료 - orderNumber: {}, trackingNumber: {}, newStatus: {}",
                    request.getOrderNumber(), normalizedTrackingNumber, order.getOrderStatus());

            // 9. 응답 생성
            return TrackingNumberRegisterResponse.successWithValidation(
                    request.getOrderNumber(),
                    normalizedTrackingNumber,
                    request.getCourierCompany(),
                    order.getOrderStatus(),
                    shipment.getShippedAt(),
                    validationResult
            );

        } catch (NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            log.warn("운송장 번호 등록 실패 - orderNumber: {}, reason: {}",
                    request.getOrderNumber(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("운송장 번호 등록 중 예상치 못한 오류 발생 - orderNumber: {}",
                    request.getOrderNumber(), e);
            throw new RuntimeException("운송장 번호 등록 중 서버 오류가 발생했습니다", e);
        }
    }

    // ===== 미구현 메서드들 (TODO 상태 유지) =====

    @Override
    public SellerOrderListResponse getSellerOrdersByStatus(UserPrincipal userPrincipal, OrderStatus orderStatus, Pageable pageable) {
        // TODO: 구현 예정
        throw new UnsupportedOperationException("구현 예정");
    }

    @Override
    public SellerOrderListResponse searchSellerOrders(UserPrincipal userPrincipal, String searchType, String searchKeyword, Pageable pageable) {
        // TODO: 구현 예정
        throw new UnsupportedOperationException("구현 예정");
    }

    @Override
    public boolean hideOrderFromList(UserPrincipal userPrincipal, String orderNumber) {
        // TODO: 구현 예정
        throw new UnsupportedOperationException("구현 예정");
    }

    // ===== Private Helper Methods =====

    private Sellers findSellerByPrincipal(UserPrincipal userPrincipal) {
        Users user = userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));

        return sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("판매자 권한이 없습니다"));
    }

    private void validatePageable(Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("페이지 크기는 100을 초과할 수 없습니다");
        }
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다");
        }
    }

    private void validateStatusTransition(OrderStatusUpdateRequest request, OrderStatus currentStatus) {
        if (!request.isValidStatusTransition(currentStatus)) {
            throw new IllegalArgumentException(
                    String.format("잘못된 상태 전환입니다. %s에서 %s로 변경할 수 없습니다",
                            currentStatus, request.getNewStatus()));
        }
    }

    private void validateReasonRequirement(OrderStatusUpdateRequest request) {
        if (request.isReasonRequired() &&
            (request.getReason() == null || request.getReason().trim().isEmpty())) {
            throw new IllegalArgumentException("해당 상태 변경에는 사유가 필수입니다");
        }
    }

    private void handleSpecialStatusChange(OrderStatusUpdateRequest request, Shipments shipment) {
        // 특별한 상태 변경 처리 (지연, 취소 등)
        if (request.isDelayRequest()) {
            // 출고 지연 처리 로직
            log.info("출고 지연 처리 - orderNumber: {}, reason: {}",
                    request.getOrderNumber(), request.getReason());
        }
    }

    private void validateTrackingNumberRegistration(Orders order, Shipments shipment, TrackingNumberRegisterRequest request) {
        // 주문 상태 확인
        if (order.getOrderStatus() != OrderStatus.READY_FOR_SHIPMENT) {
            throw new IllegalStateException("배송 준비 완료 상태에서만 운송장을 등록할 수 있습니다");
        }

        // 이미 운송장이 등록된 경우
        if (shipment.isShipped()) {
            throw new IllegalStateException("이미 운송장이 등록된 주문입니다");
        }
    }

    private void validateTrackingNumberDuplication(TrackingNumberRegisterRequest request) {
        String normalizedTrackingNumber = request.getNormalizedTrackingNumber();
        String courierDisplayName = request.getCourierDisplayName();

        if (shipmentRepository.existsByCourierAndTrackingNumber(courierDisplayName, normalizedTrackingNumber)) {
            throw new IllegalStateException("이미 등록된 운송장 번호입니다");
        }
    }

    private TrackingNumberRegisterResponse.ValidationResult validateTrackingNumberWithApi(TrackingNumberRegisterRequest request) {
        if (!request.shouldValidateWithApi()) {
            return TrackingNumberRegisterResponse.ValidationResult.skipped("API 검증을 생략했습니다");
        }

        // TODO: 스마트택배 API 연동 구현
        // 현재는 성공으로 가정
        return TrackingNumberRegisterResponse.ValidationResult.success("운송장 번호가 유효합니다");
    }

    // ===== DTO 변환 메서드들 =====

    private SellerOrderDetailResponse buildSellerOrderDetailResponse(
            Shipments shipment, List<OrderItems> orderItems, String sellerId) {
        // TODO: DTO 변환 로직 구현
        throw new UnsupportedOperationException("DTO 변환 로직 구현 예정");
    }

    private SellerOrderListResponse.SellerOrderSummary convertToSellerOrderSummary(Shipments shipment) {
        // TODO: DTO 변환 로직 구현
        throw new UnsupportedOperationException("DTO 변환 로직 구현 예정");
    }
}