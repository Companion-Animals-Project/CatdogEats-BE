package com.team5.catdogeats.orders.service.seller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 판매자용 주문 목록 관리 서비스
 * 단일 책임: 주문 목록 조회, 검색, 필터링
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderListService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final ShipmentRepository shipmentRepository;

    /**
     * 판매자용 주문 목록 조회 (전체)
     * @param userPrincipal 인증된 판매자 정보
     * @param pageable 페이징 정보
     * @return 주문 목록
     */
    @JpaTransactional(readOnly = true)
    public SellerOrderListResponse getSellerOrders(UserPrincipal userPrincipal, Pageable pageable) {
        try {
            log.debug("판매자 주문 목록 조회 - provider={}, providerId={}, page={}, size={}",
                    userPrincipal.provider(), userPrincipal.providerId(),
                    pageable.getPageNumber(), pageable.getPageSize());

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 페이징 정보 검증
            validatePageable(pageable);

            // 3. 주문 목록 조회
            Page<Shipments> shipmentPage = shipmentRepository
                    .findSellerOrdersWithPaging(seller.getUserId(), pageable);

            // 4. DTO 변환
            List<SellerOrderListResponse.SellerOrderSummary> orderSummaries =
                    shipmentPage.getContent().stream()
                            .map(this::convertToSellerOrderSummary)
                            .toList();

            // 5. 응답 생성
            return SellerOrderListResponse.of(
                    orderSummaries,
                    shipmentPage.getNumber(),
                    shipmentPage.getTotalPages(),
                    shipmentPage.getTotalElements(),
                    shipmentPage.getSize(),
                    shipmentPage.hasNext(),
                    shipmentPage.hasPrevious()
            );

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("주문 목록 조회 실패 - reason: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 목록 조회 중 예상치 못한 오류 발생", e);
            throw new RuntimeException("주문 목록 조회 중 서버 오류가 발생했습니다", e);
        }
    }

    /**
     * 판매자용 주문 목록 조회 - 상태 필터링
     * @param userPrincipal 인증된 판매자 정보
     * @param orderStatus 필터링할 주문 상태
     * @param pageable 페이징 정보
     * @return 상태별 필터링된 주문 목록
     */
    @JpaTransactional(readOnly = true)
    public SellerOrderListResponse getSellerOrdersByStatus(
            UserPrincipal userPrincipal,
            OrderStatus orderStatus,
            Pageable pageable) {

        try {
            log.debug("판매자 주문 목록 조회 (상태 필터링) - provider={}, providerId={}, status={}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderStatus);

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 페이징 정보 검증
            validatePageable(pageable);

            // 3. 상태별 주문 목록 조회
            Page<Shipments> shipmentPage = shipmentRepository
                    .findSellerOrdersByStatusWithPaging(seller.getUserId(), orderStatus, pageable);

            // 4. DTO 변환
            List<SellerOrderListResponse.SellerOrderSummary> orderSummaries =
                    shipmentPage.getContent().stream()
                            .map(this::convertToSellerOrderSummary)
                            .toList();

            // 5. 응답 생성
            return SellerOrderListResponse.filteredResult(
                    orderSummaries,
                    shipmentPage.getNumber(),
                    shipmentPage.getTotalPages(),
                    shipmentPage.getTotalElements(),
                    shipmentPage.getSize(),
                    shipmentPage.hasNext(),
                    shipmentPage.hasPrevious(),
                    orderStatus
            );

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("판매자용 주문 목록 조회 (상태 필터링) 실패 - reason: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("판매자용 주문 목록 조회 (상태 필터링) 중 예상치 못한 오류 발생", e);
            throw new RuntimeException("주문 목록 조회 중 서버 오류가 발생했습니다", e);
        }
    }

    /**
     * 판매자용 주문 검색
     * @param userPrincipal 인증된 판매자 정보
     * @param searchType 검색 타입 (orderNumber, recipientName)
     * @param searchKeyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    @JpaTransactional(readOnly = true)
    public SellerOrderListResponse searchSellerOrders(
            UserPrincipal userPrincipal,
            String searchType,
            String searchKeyword,
            Pageable pageable) {

        try {
            log.debug("판매자 주문 검색 - provider={}, providerId={}, searchType={}, keyword={}",
                    userPrincipal.provider(), userPrincipal.providerId(), searchType, searchKeyword);

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 페이징 정보 검증
            validatePageable(pageable);

            // 3. 검색 조건 검증
            validateSearchConditions(searchType, searchKeyword);

            // 4. 검색 타입별 조회
            Page<Shipments> shipmentPage = performSearch(seller.getUserId(), searchType, searchKeyword, pageable);

            // 5. DTO 변환
            List<SellerOrderListResponse.SellerOrderSummary> orderSummaries =
                    shipmentPage.getContent().stream()
                            .map(this::convertToSellerOrderSummary)
                            .toList();

            // 6. 응답 생성
            return SellerOrderListResponse.searchResult(
                    orderSummaries,
                    shipmentPage.getNumber(),
                    shipmentPage.getTotalPages(),
                    shipmentPage.getTotalElements(),
                    shipmentPage.getSize(),
                    shipmentPage.hasNext(),
                    shipmentPage.hasPrevious(),
                    searchType,
                    searchKeyword
            );

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("판매자 주문 검색 실패 - reason: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("판매자 주문 검색 중 예상치 못한 오류 발생", e);
            throw new RuntimeException("주문 검색 중 서버 오류가 발생했습니다", e);
        }
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

    private void validateSearchConditions(String searchType, String searchKeyword) {
        if (searchType == null || searchType.trim().isEmpty()) {
            throw new IllegalArgumentException("검색 타입은 필수입니다");
        }

        if (searchKeyword == null || searchKeyword.trim().isEmpty()) {
            throw new IllegalArgumentException("검색 키워드는 필수입니다");
        }

        if (searchKeyword.trim().length() < 2) {
            throw new IllegalArgumentException("검색 키워드는 2자 이상이어야 합니다");
        }

        if (!isValidSearchType(searchType)) {
            throw new IllegalArgumentException("지원하지 않는 검색 타입입니다. (orderNumber, recipientName 만 지원)");
        }
    }

    private boolean isValidSearchType(String searchType) {
        return "orderNumber".equals(searchType) || "recipientName".equals(searchType);
    }

    private Page<Shipments> performSearch(String sellerId, String searchType, String searchKeyword, Pageable pageable) {
        return switch (searchType) {
            case "orderNumber" -> shipmentRepository.findSellerOrdersByOrderNumberWithPaging(
                    sellerId, searchKeyword, pageable);
            case "recipientName" -> shipmentRepository.findSellerOrdersByRecipientNameWithPaging(
                    sellerId, searchKeyword, pageable);
            default -> throw new IllegalArgumentException("지원하지 않는 검색 타입: " + searchType);
        };
    }

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

    private SellerOrderListResponse.SellerOrderItem convertToSellerOrderItem(OrderItems orderItem) {
        Products product = orderItem.getProducts();
        return SellerOrderListResponse.SellerOrderItem.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .productImageUrl(product.getImageUrl())
                .unitPrice(orderItem.getUnitPrice())
                .quantity(orderItem.getQuantity())
                .totalPrice(orderItem.getUnitPrice() * orderItem.getQuantity())
                .productOptions(orderItem.getProductOptions())
                .build();
    }

    private String buildBasicAddress(Shipments shipment) {
        return shipment.getAddress() == null ? "" : shipment.getAddress();
    }

    private boolean canChangeOrderStatus(OrderStatus status) {
        return switch (status) {
            case PAYMENT_COMPLETED, PREPARING, READY_FOR_SHIPMENT -> true;
            default -> false;
        };
    }

    private String generateNextAction(OrderStatus status, Shipments shipment) {
        return switch (status) {
            case PAYMENT_COMPLETED -> "상품 준비를 시작하고 '상품준비중' 상태로 변경해주세요";
            case PREPARING -> "상품 포장이 완료되면 '배송준비완료' 상태로 변경해주세요";
            case READY_FOR_SHIPMENT -> shipment.isShipped() ?
                    "배송이 진행 중입니다" : "택배사에 접수 후 운송장 번호를 등록해주세요";
            case IN_DELIVERY -> "배송이 완료되면 자동으로 상태가 변경됩니다";
            case DELIVERED -> "배송이 완료되었습니다";
            case CANCELLED -> "주문이 취소되었습니다";
            default -> "추가 작업이 필요하지 않습니다";
        };
    }
}