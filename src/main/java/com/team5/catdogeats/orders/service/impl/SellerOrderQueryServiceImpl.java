package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.SellerOrderQueryService;
import com.team5.catdogeats.orders.util.ShippingAddressUtils;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

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
            List<OrderItems> sellerOrderItems = filterSellerOrderItems(order.getOrderItems(), seller.getUserId());

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
     * 주문번호로 배송 정보 조회 - 기존 메서드 활용
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
     * 판매자 소유 상품만 필터링 - 기존 메서드명 활용
     */
    private List<OrderItems> filterSellerOrderItems(List<OrderItems> orderItems, String sellerId) {
        return orderItems.stream()
                .filter(item -> item.getProducts().getSeller().getUserId().equals(sellerId))
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

        // 정렬 속성 검증 추가
        validateSortProperties(pageable.getSort());
    }
    /**
     * 정렬 속성 검증
     * Shipments 엔티티에서 사용 가능한 속성만 허용
     */
    private void validateSortProperties(Sort sort) {
        // 허용되는 정렬 속성들
        Set<String> allowedProperties = Set.of(
                "createdAt", "updatedAt", "shippingStatus",
                "shippingCompany", "trackingNumber"
        );

        for (Sort.Order order : sort) {
            String property = order.getProperty();
            if (!allowedProperties.contains(property)) {
                throw new IllegalArgumentException(
                        String.format("정렬 속성 '%s'는 지원되지 않습니다. 사용 가능한 속성: %s",
                                property, allowedProperties)
                );
            }
        }
    }
    /**
     * 주문 상세 응답 DTO 생성
     */
    private SellerOrderDetailResponse buildDetailResponse(Orders order, Shipments shipment, List<OrderItems> sellerOrderItems) {
        // 배송지 정보 생성
        SellerOrderDetailResponse.ShippingAddress shippingAddress = buildShippingAddress(shipment);

        // 주문 상품 정보 변환 - 기존 메서드명 활용
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
        String maskedPhone = maskPhoneNumber(shipment.getRecipientPhone());
        String fullAddress = ShippingAddressUtils.buildFullAddress(
                shipment.getStreetAddress(),
                shipment.getDetailAddress()
        );

        return SellerOrderDetailResponse.ShippingAddress.builder()
                .recipientName(shipment.getRecipientName())
                .recipientPhone(shipment.getRecipientPhone())
                .maskedPhone(maskedPhone)
                .zipCode(shipment.getPostalCode())
                .address(shipment.getStreetAddress())
                .addressDetail(shipment.getDetailAddress())
                .fullAddress(fullAddress)
                .deliveryRequest(shipment.getDeliveryRequest())
                .build();
    }

    /**
     * 주문 상품 상세 정보 변환 - 기존 메서드명 활용
     */
    private SellerOrderDetailResponse.SellerOrderDetailItem convertToOrderDetailItem(OrderItems orderItem) {
        return SellerOrderDetailResponse.SellerOrderDetailItem.builder()
                .orderItemId(orderItem.getId())
                .productId(orderItem.getProducts().getId())
                .productName(orderItem.getProducts().getTitle()) // 기존 메서드 활용
                .productImage(null) // 이미지 관련 기능은 별도 구현 필요
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getPrice()) // 기존 메서드 활용
                .totalPrice(orderItem.getPrice() * orderItem.getQuantity())
                .vendorName(orderItem.getProducts().getSeller().getVendorName())
                .build();
    }

    /**
     * 주문 요약 정보 생성
     */
    private SellerOrderDetailResponse.OrderSummary buildOrderSummary(List<OrderItems> sellerOrderItems) {
        int itemCount = sellerOrderItems.size();
        Long totalProductPrice = sellerOrderItems.stream()
                .mapToLong(item -> item.getPrice() * item.getQuantity())
                .sum();

        return SellerOrderDetailResponse.OrderSummary.builder()
                .itemCount(itemCount)
                .totalProductPrice(totalProductPrice)
                .deliveryFee(0L) // 배송비 로직 추후 구현
                .totalAmount(totalProductPrice)
                .build();
    }

    /**
     * 배송 정보 생성
     */
    private SellerOrderDetailResponse.ShipmentInfo buildShipmentInfo(Shipments shipment) {
        boolean isShipped = shipment.getShippedAt() != null;

        return SellerOrderDetailResponse.ShipmentInfo.builder()
                .courier(shipment.getCourier())
                .trackingNumber(shipment.getTrackingNumber())
                .shippedAt(shipment.getShippedAt())
                .deliveredAt(shipment.getDeliveredAt())
                .isShipped(isShipped)
                .shipmentMemo(shipment.getShipmentMemo())
                .build();
    }

    /**
     * 상태 관리 정보 생성
     */
    private SellerOrderDetailResponse.StatusManagement buildStatusManagement(Orders order, Shipments shipment) {
        return SellerOrderDetailResponse.StatusManagement.builder()
                .canChangeStatus(true) // 상태 변경 가능 여부 로직 추후 구현
                .availableStatuses(List.of()) // 변경 가능한 상태 목록 추후 구현
                .canRegisterTracking(shipment.getTrackingNumber() == null)
                .statusDescription("상태 설명 추후 구현")
                .build();
    }

    /**
     * 주문 목록 요약 정보 변환
     */
    private SellerOrderListResponse.SellerOrderSummary convertToSellerOrderSummary(Shipments shipment) {
        Orders order = shipment.getOrders();

        // 해당 판매자의 상품만 필터링
        List<OrderItems> sellerOrderItems = order.getOrderItems().stream()
                .filter(item -> item.getProducts().getSeller().getUserId().equals(shipment.getSeller().getUserId()))
                .toList();

        // 주문 상품 정보 변환
        List<SellerOrderListResponse.SellerOrderItem> orderItems = sellerOrderItems.stream()
                .map(this::convertToSellerOrderItem)
                .toList();

        // 주문 요약 정보
        SellerOrderListResponse.OrderSummaryInfo orderSummary = buildOrderSummaryInfo(sellerOrderItems);

        // 배송 기본 정보
        SellerOrderListResponse.ShipmentBasicInfo shipmentInfo = buildShipmentBasicInfo(shipment);

        return SellerOrderListResponse.SellerOrderSummary.builder()
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getCreatedAt())
                .buyerName(order.getUser().getName())
                .maskedBuyerName(maskName(order.getUser().getName()))
                .orderItems(orderItems)
                .orderSummary(orderSummary)
                .shipmentInfo(shipmentInfo)
                .build();
    }

    /**
     * 판매자 주문 상품 정보 변환 - 기존 메서드명 활용
     */
    private SellerOrderListResponse.SellerOrderItem convertToSellerOrderItem(OrderItems orderItem) {
        return SellerOrderListResponse.SellerOrderItem.builder()
                .orderItemId(orderItem.getId())
                .productId(orderItem.getProducts().getId())
                .productName(orderItem.getProducts().getTitle()) // 기존 메서드 활용
                .productImage(null) // 이미지 관련 기능은 별도 구현 필요
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getPrice()) // 기존 메서드 활용
                .totalPrice(orderItem.getPrice() * orderItem.getQuantity())
                .build();
    }

    /**
     * 주문 요약 정보 생성 (목록용)
     */
    private SellerOrderListResponse.OrderSummaryInfo buildOrderSummaryInfo(List<OrderItems> sellerOrderItems) {
        int itemCount = sellerOrderItems.size();
        Long totalAmount = sellerOrderItems.stream()
                .mapToLong(item -> item.getPrice() * item.getQuantity())
                .sum();

        return SellerOrderListResponse.OrderSummaryInfo.builder()
                .itemCount(itemCount)
                .totalAmount(totalAmount)
                .build();
    }

    /**
     * 배송 기본 정보 생성 (목록용)
     */
    private SellerOrderListResponse.ShipmentBasicInfo buildShipmentBasicInfo(Shipments shipment) {
        boolean isShipped = shipment.getShippedAt() != null;

        return SellerOrderListResponse.ShipmentBasicInfo.builder()
                .courier(shipment.getCourier())
                .trackingNumber(shipment.getTrackingNumber())
                .isShipped(isShipped)
                .shippedAt(shipment.getShippedAt())
                .build();
    }

    // ===== Utility Methods =====

    /**
     * 전화번호 마스킹
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }

    /**
     * 이름 마스킹
     */
    private String maskName(String name) {
        if (name == null || name.length() < 2) {
            return "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}