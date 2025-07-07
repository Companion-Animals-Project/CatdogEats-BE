package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.SellerOrderQueryService;
import com.team5.catdogeats.products.domain.Products;
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
 * 판매자용 주문 읽기 전용 서비스 구현체 (CQRS Query)
 * 단일 책임: 주문 조회 관련 기능만 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderQueryServiceImpl implements SellerOrderQueryService {

    private final UserRepository userRepository;
    private final SellersRepository sellerRepository;
    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    /**
     * 판매자용 주문 상세 조회 (배송지 정보 포함)
     */
    @Override
    @JpaTransactional(readOnly = true)
    public SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, String orderNumber) {
        try {
            log.debug("판매자 주문 상세 조회 시작 - provider={}, providerId={}, orderNumber={}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 확인
            Shipments shipment = findShipmentByOrderNumber(orderNumber);
            Orders order = shipment.getOrders();

            // 3. 판매자 소유 상품 확인
            validateSellerOwnership(seller, order);

            // 4. 해당 판매자의 상품만 필터링
            List<OrderItems> sellerOrderItems = filterSellerOrderItems(order.getOrderItems(), seller.getId());

            // 5. 응답 DTO 생성
            SellerOrderDetailResponse response = buildDetailResponse(order, shipment, sellerOrderItems);

            log.info("판매자 주문 상세 조회 성공 - orderNumber={}, sellerItemCount={}, totalAmount={}",
                    orderNumber, sellerOrderItems.size(), response.getTotalAmount());

            return response;

        } catch (NoSuchElementException e) {
            log.warn("판매자 주문 상세 조회 실패 - orderNumber={}, reason={}", orderNumber, e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("판매자 주문 상세 조회 권한 오류 - orderNumber={}, reason={}", orderNumber, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("판매자 주문 상세 조회 중 오류 - orderNumber={}", orderNumber, e);
            throw new RuntimeException("주문 상세 조회 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 판매자용 주문 목록 조회 (단순 페이징)
     */
    @Override
    @JpaTransactional(readOnly = true)
    public SellerOrderListResponse getSellerOrders(UserPrincipal userPrincipal, Pageable pageable) {
        try {
            log.debug("판매자 주문 목록 조회 시작 - provider={}, providerId={}, page={}, size={}",
                    userPrincipal.provider(), userPrincipal.providerId(),
                    pageable.getPageNumber(), pageable.getPageSize());

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 페이징 정보 검증
            validatePageable(pageable);

            // 3. 주문 목록 조회 (숨김 처리된 주문 제외)
            Page<Shipments> shipmentPage = shipmentRepository
                    .findSellerOrdersWithPaging(seller.getUserId(), pageable);

            // 4. DTO 변환
            List<SellerOrderListResponse.SellerOrderSummary> orderSummaries =
                    shipmentPage.getContent().stream()
                            .map(this::convertToSellerOrderSummary)
                            .toList();

            // 5. 응답 DTO 생성
            SellerOrderListResponse response = SellerOrderListResponse.builder()
                    .orders(orderSummaries)
                    .currentPage(shipmentPage.getNumber())
                    .totalPages(shipmentPage.getTotalPages())
                    .totalElements(shipmentPage.getTotalElements())
                    .pageSize(shipmentPage.getSize())
                    .hasNext(shipmentPage.hasNext())
                    .hasPrevious(shipmentPage.hasPrevious())
                    .searchType(null)
                    .searchKeyword(null)
                    .filterStatus(null)
                    .build();

            log.info("판매자 주문 목록 조회 성공 - page={}, size={}, totalElements={}",
                    pageable.getPageNumber(), pageable.getPageSize(), shipmentPage.getTotalElements());

            return response;

        } catch (IllegalArgumentException e) {
            log.warn("판매자 주문 목록 조회 인자 오류 - reason={}", e.getMessage());
            throw e;
        } catch (NoSuchElementException e) {
            log.warn("판매자 주문 목록 조회 실패 - reason={}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("판매자 주문 목록 조회 중 오류 - provider={}, providerId={}",
                    userPrincipal.provider(), userPrincipal.providerId(), e);
            throw new RuntimeException("주문 목록 조회 중 오류가 발생했습니다", e);
        }
    }

    // ===== Helper Methods =====

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
     * 주문번호로 배송 정보 조회
     */
    private Shipments findShipmentByOrderNumber(String orderNumber) {
        return shipmentRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderNumber));
    }

    /**
     * 판매자 소유 상품 확인
     */
    private void validateSellerOwnership(Sellers seller, Orders order) {
        boolean hasSellerProducts = order.getOrderItems().stream()
                .anyMatch(item -> item.getProducts().getSeller().getId().equals(seller.getId()));

        if (!hasSellerProducts) {
            throw new IllegalArgumentException("해당 주문에 대한 접근 권한이 없습니다");
        }
    }

    /**
     * 판매자 소유 상품만 필터링
     */
    private List<OrderItems> filterSellerOrderItems(List<OrderItems> orderItems, String sellerId) {
        return orderItems.stream()
                .filter(item -> item.getProducts().getSeller().getId().equals(sellerId))
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

    /**
     * 주문 상세 응답 DTO 생성
     */
    private SellerOrderDetailResponse buildDetailResponse(Orders order, Shipments shipment, List<OrderItems> sellerOrderItems) {
        // 배송지 정보 생성
        SellerOrderDetailResponse.ShippingAddress shippingAddress = buildShippingAddress(shipment);

        // 주문 상품 정보 변환
        List<SellerOrderDetailResponse.SellerOrderDetailItem> orderItems =
                sellerOrderItems.stream()
                        .map(this::convertToOrderDetailItem)
                        .toList();

        // 주문 요약 정보 생성
        SellerOrderDetailResponse.OrderSummary orderSummary = buildOrderSummary(sellerOrderItems);

        // 배송 정보 생성
        SellerOrderDetailResponse.ShipmentInfo shipmentInfo = buildShipmentInfo(shipment);

        // 상태 관리 정보 생성
        SellerOrderDetailResponse.StatusManagement statusManagement = buildStatusManagement(order, shipment);

        return SellerOrderDetailResponse.builder()
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getCreatedAt())
                .shippingAddress(shippingAddress)
                .orderItems(orderItems)
                .orderSummary(orderSummary)
                .shipmentInfo(shipmentInfo)
                .statusManagement(statusManagement)
                .build();
    }

    /**
     * 배송지 정보 생성
     */
    private SellerOrderDetailResponse.ShippingAddress buildShippingAddress(Shipments shipment) {
        return SellerOrderDetailResponse.ShippingAddress.builder()
                .recipientName(shipment.getRecipientName())
                .recipientPhone(shipment.getRecipientPhone())
                .maskedPhone(shipment.getMaskedPhone())
                .zipCode(shipment.getZipCode())
                .address(shipment.getAddress())
                .addressDetail(shipment.getAddressDetail())
                .fullAddress(shipment.getFullAddress())
                .deliveryRequest(shipment.getDeliveryRequest())
                .build();
    }

    /**
     * 주문 상품 상세 정보 변환
     */
    private SellerOrderDetailResponse.SellerOrderDetailItem convertToOrderDetailItem(OrderItems orderItem) {
        Products product = orderItem.getProducts();

        return SellerOrderDetailResponse.SellerOrderDetailItem.builder()
                .orderItemId(orderItem.getId())
                .productId(product.getId())
                .productName(product.getProductName())
                .productImage(product.getProductMainImage())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .totalPrice(orderItem.getUnitPrice() * orderItem.getQuantity())
                .build();
    }

    /**
     * 주문 요약 정보 생성
     */
    private SellerOrderDetailResponse.OrderSummary buildOrderSummary(List<OrderItems> sellerOrderItems) {
        long totalPrice = sellerOrderItems.stream()
                .mapToLong(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        return SellerOrderDetailResponse.OrderSummary.builder()
                .itemCount(sellerOrderItems.size())
                .totalPrice(totalPrice)
                .build();
    }

    /**
     * 배송 정보 생성
     */
    private SellerOrderDetailResponse.ShipmentInfo buildShipmentInfo(Shipments shipment) {
        return SellerOrderDetailResponse.ShipmentInfo.builder()
                .courier(shipment.getCourier())
                .trackingNumber(shipment.getTrackingNumber())
                .shippedAt(shipment.getShippedAt())
                .deliveredAt(shipment.getDeliveredAt())
                .trackingUpdatedAt(shipment.getTrackingUpdatedAt())
                .isShipped(shipment.isShipped())
                .isDelivered(shipment.isDelivered())
                .build();
    }

    /**
     * 상태 관리 정보 생성
     */
    private SellerOrderDetailResponse.StatusManagement buildStatusManagement(Orders order, Shipments shipment) {
        boolean canChangeStatus = canChangeOrderStatus(order.getOrderStatus());
        boolean requiresTracking = order.getOrderStatus() == OrderStatus.READY_FOR_SHIPMENT && !shipment.isShipped();
        String nextAction = generateNextAction(order.getOrderStatus(), shipment);

        return SellerOrderDetailResponse.StatusManagement.builder()
                .canChangeStatus(canChangeStatus)
                .requiresTracking(requiresTracking)
                .nextAction(nextAction)
                .availableStatuses(getAvailableStatuses(order.getOrderStatus()))
                .build();
    }

    /**
     * 주문 목록 요약 정보 변환
     */
    private SellerOrderListResponse.SellerOrderSummary convertToSellerOrderSummary(Shipments shipment) {
        Orders order = shipment.getOrders();

        // 해당 판매자의 상품만 필터링 (Repository에서 이미 필터링됨)
        List<OrderItems> sellerOrderItems = order.getOrderItems();

        // 상품 정보 변환
        List<SellerOrderListResponse.SellerOrderItem> orderItems =
                sellerOrderItems.stream()
                        .map(this::convertToSellerOrderItem)
                        .toList();

        // 총 가격 계산
        Long totalPrice = sellerOrderItems.stream()
                .mapToLong(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        // 상태 관리 정보
        boolean canChangeStatus = canChangeOrderStatus(order.getOrderStatus());
        boolean requiresTracking = order.getOrderStatus() == OrderStatus.READY_FOR_SHIPMENT &&
                !shipment.isShipped();
        String nextAction = generateNextAction(order.getOrderStatus(), shipment);

        return SellerOrderListResponse.SellerOrderSummary.builder()
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getCreatedAt())
                .recipientName(shipment.getRecipientName())
                .maskedPhone(shipment.getMaskedPhone())
                .basicAddress(buildBasicAddress(shipment))
                .orderItems(orderItems)
                .totalPrice(totalPrice)
                .itemCount(orderItems.size())
                .courier(shipment.getCourier())
                .trackingNumber(shipment.getTrackingNumber())
                .shippedAt(shipment.getShippedAt())
                .deliveredAt(shipment.getDeliveredAt())
                .canChangeStatus(canChangeStatus)
                .requiresTracking(requiresTracking)
                .nextAction(nextAction)
                .build();
    }

    /**
     * 주문 상품 목록 변환
     */
    private SellerOrderListResponse.SellerOrderItem convertToSellerOrderItem(OrderItems orderItem) {
        Products product = orderItem.getProducts();

        return SellerOrderListResponse.SellerOrderItem.builder()
                .orderItemId(orderItem.getId())
                .productId(product.getId())
                .productName(product.getProductName())
                .productImage(product.getProductMainImage())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .totalPrice(orderItem.getUnitPrice() * orderItem.getQuantity())
                .build();
    }

    /**
     * 기본 주소 생성 (상세주소 제외)
     */
    private String buildBasicAddress(Shipments shipment) {
        return String.format("%s %s %s %s",
                shipment.getCity(),
                shipment.getDistrict(),
                shipment.getNeighborhood(),
                shipment.getAddress()).trim();
    }

    /**
     * 상태 변경 가능 여부 확인
     */
    private boolean canChangeOrderStatus(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case PAYMENT_COMPLETED, PREPARING, READY_FOR_SHIPMENT -> true;
            case IN_DELIVERY, DELIVERED, CANCELLED -> false;
        };
    }

    /**
     * 다음 수행 작업 안내 생성
     */
    private String generateNextAction(OrderStatus orderStatus, Shipments shipment) {
        return switch (orderStatus) {
            case PAYMENT_COMPLETED -> "상품 준비를 시작하세요";
            case PREPARING -> "배송 준비를 완료하세요";
            case READY_FOR_SHIPMENT -> shipment.isShipped() ? "배송이 곧 시작됩니다" : "운송장 번호를 등록하세요";
            case IN_DELIVERY -> "배송이 진행 중입니다";
            case DELIVERED -> "배송이 완료되었습니다";
            case CANCELLED -> "주문이 취소되었습니다";
        };
    }

    /**
     * 사용 가능한 상태 목록 반환
     */
    private List<OrderStatus> getAvailableStatuses(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case PAYMENT_COMPLETED -> List.of(OrderStatus.PREPARING, OrderStatus.CANCELLED);
            case PREPARING -> List.of(OrderStatus.READY_FOR_SHIPMENT, OrderStatus.CANCELLED);
            case READY_FOR_SHIPMENT -> List.of(OrderStatus.IN_DELIVERY);
            default -> List.of();
        };
    }
}