package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.CourierCompany;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.request.OrderStatusUpdateRequest;
import com.team5.catdogeats.orders.dto.request.TrackingNumberRegisterRequest;
import com.team5.catdogeats.orders.dto.response.*;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.SellerOrderService;
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

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 판매자용 주문 관리 서비스 구현체
 * (목록/상세 조회, 상태 변경, 운송장 등록, 숨김 처리, 검색 등)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderServiceImpl implements SellerOrderService {

    private final UserRepository     userRepository;
    private final SellerRepository   sellerRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository    orderRepository;

    /* =========================================================================
     *  주문 상세 조회
     * =========================================================================
     */
    @Override
    public SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, String orderNumber) {

        try {
            log.debug("판매자 주문 상세 조회 시작 - provider={}, providerId={}, orderNumber={}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

            // 1. 판매자 확인
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 배송정보 + 권한 검증
            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(orderNumber, seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            // 3. 해당 판매자 상품 목록
            List<OrderItems> sellerOrderItems = shipment.getOrders().getOrderItems();

            // 4. DTO 변환
            return buildSellerOrderDetailResponse(shipment, sellerOrderItems, seller.getUserId());

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("주문 상세 조회 실패 - orderNumber={}, reason={}", orderNumber, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 상세 조회 중 오류 - orderNumber={}", orderNumber, e);
            throw new RuntimeException("주문 상세 조회 중 서버 오류가 발생했습니다", e);
        }
    }

    /* =========================================================================
     *  주문 목록(전체)
     * =========================================================================
     */
    @Override
    public SellerOrderListResponse getSellerOrders(UserPrincipal userPrincipal, Pageable pageable) {

        try {
            log.debug("판매자 주문 목록 조회 - provider={}, providerId={}, page={}, size={}",
                    userPrincipal.provider(), userPrincipal.providerId(),
                    pageable.getPageNumber(), pageable.getPageSize());

            Sellers seller = findSellerByPrincipal(userPrincipal);
            validatePageable(pageable);

            Page<Shipments> shipmentPage =
                    shipmentRepository.findSellerOrdersWithPaging(seller.getUserId(), pageable);

            List<SellerOrderListResponse.SellerOrderSummary> summaries =
                    shipmentPage.getContent().stream()
                            .map(this::convertToSellerOrderSummary)
                            .toList();

            return SellerOrderListResponse.builder()
                    .orders(summaries)
                    .currentPage(shipmentPage.getNumber())
                    .totalPages(shipmentPage.getTotalPages())
                    .totalElements(shipmentPage.getTotalElements())
                    .pageSize(shipmentPage.getSize())
                    .hasNext(shipmentPage.hasNext())
                    .hasPrevious(shipmentPage.hasPrevious())
                    .build();

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("주문 목록 조회 실패 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 목록 조회 중 오류", e);
            throw new RuntimeException("주문 목록 조회 중 서버 오류가 발생했습니다", e);
        }
    }

    /* =========================================================================
     *  주문 목록(상태별)
     * =========================================================================
     */
    @Override
    @JpaTransactional(readOnly = true)
    public SellerOrderListResponse getSellerOrdersByStatus(
            UserPrincipal userPrincipal,
            OrderStatus orderStatus,
            Pageable pageable) {

        try {
            log.debug("판매자 주문 목록(상태) 조회 - provider={}, providerId={}, status={}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderStatus);

            Sellers seller = findSellerByPrincipal(userPrincipal);
            validatePageable(pageable);

            Page<Shipments> shipmentPage =
                    shipmentRepository.findSellerOrdersByStatusWithPaging(
                            seller.getUserId(), orderStatus, pageable);

            List<SellerOrderListResponse.SellerOrderSummary> summaries =
                    shipmentPage.getContent().stream()
                            .map(this::convertToSellerOrderSummary)
                            .toList();

            return SellerOrderListResponse.filteredResult(
                    summaries,
                    shipmentPage.getNumber(),
                    shipmentPage.getTotalPages(),
                    shipmentPage.getTotalElements(),
                    shipmentPage.getSize(),
                    shipmentPage.hasNext(),
                    shipmentPage.hasPrevious(),
                    orderStatus
            );

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("주문 목록(상태) 조회 실패 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 목록(상태) 조회 중 오류", e);
            throw new RuntimeException("주문 목록 조회 중 서버 오류가 발생했습니다", e);
        }
    }

    /* =========================================================================
     *  주문 검색
     * =========================================================================
     */
    @Override
    @JpaTransactional(readOnly = true)
    public SellerOrderListResponse searchSellerOrders(
            UserPrincipal userPrincipal,
            String searchType,
            String searchKeyword,
            Pageable pageable) {

        try {
            log.debug("판매자 주문 검색 - provider={}, providerId={}, type={}, keyword={}",
                    userPrincipal.provider(), userPrincipal.providerId(), searchType, searchKeyword);

            Sellers seller = findSellerByPrincipal(userPrincipal);
            validatePageable(pageable);
            validateSearchConditions(searchType, searchKeyword);

            Page<Shipments> shipmentPage =
                    performSearch(seller.getUserId(), searchType, searchKeyword, pageable);

            List<SellerOrderListResponse.SellerOrderSummary> summaries =
                    shipmentPage.getContent().stream()
                            .map(this::convertToSellerOrderSummary)
                            .toList();

            return SellerOrderListResponse.searchResult(
                    summaries,
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
            log.warn("주문 검색 실패 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 검색 중 오류", e);
            throw new RuntimeException("주문 검색 중 서버 오류가 발생했습니다", e);
        }
    }

    /* =========================================================================
     *  주문 상태 변경
     * =========================================================================
     */
    @Override
    @JpaTransactional
    public OrderStatusUpdateResponse updateOrderStatus(
            UserPrincipal userPrincipal,
            OrderStatusUpdateRequest request) {

        try {
            log.debug("주문 상태 변경 - orderNumber={}, newStatus={}",
                    request.getOrderNumber(), request.getNewStatus());

            Sellers seller = findSellerByPrincipal(userPrincipal);

            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(request.getOrderNumber(), seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            Orders order = shipment.getOrders();
            OrderStatus current = order.getOrderStatus();

            validateStatusTransition(request, current);
            validateReasonRequirement(request);

            order.setOrderStatus(request.getNewStatus());
            handleSpecialStatusChange(request, shipment);

            orderRepository.save(order);

            log.info("주문 상태 변경 완료 - {} -> {}", current, request.getNewStatus());

            return OrderStatusUpdateResponse.success(
                    request.getOrderNumber(), current, request.getNewStatus(), request.getReason());

        } catch (NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            log.warn("주문 상태 변경 실패 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 상태 변경 중 오류", e);
            throw new RuntimeException("주문 상태 변경 중 서버 오류가 발생했습니다", e);
        }
    }

    /* =========================================================================
     *  운송장 번호 등록
     * =========================================================================
     */
    @Override
    @JpaTransactional
    public TrackingNumberRegisterResponse registerTrackingNumber(
            UserPrincipal userPrincipal,
            TrackingNumberRegisterRequest request) {

        try {
            log.debug("운송장 등록 - orderNumber={}, courier={}, tracking={}",
                    request.getOrderNumber(), request.getCourierDisplayName(), request.getTrackingNumber());

            Sellers seller = findSellerByPrincipal(userPrincipal);

            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(request.getOrderNumber(), seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            Orders order = shipment.getOrders();

            validateTrackingNumberRegistration(order, shipment, request);
            validateTrackingNumberDuplication(request);

            var validationResult = validateTrackingNumberWithApi(request);

            String normalized = request.getNormalizedTrackingNumber();
            shipment.setTrackingInfo(request.getCourierDisplayName(), normalized);

            if (request.shouldStartShipmentImmediately()) {
                order.setOrderStatus(OrderStatus.IN_DELIVERY);
            }

            orderRepository.save(order);
            shipmentRepository.save(shipment);

            log.info("운송장 등록 완료 - trackingNumber={}", normalized);

            return TrackingNumberRegisterResponse.successWithValidation(
                    request.getOrderNumber(),
                    normalized,
                    request.getCourierCompany(),
                    order.getOrderStatus(),
                    shipment.getShippedAt(),
                    validationResult
            );

        } catch (NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            log.warn("운송장 등록 실패 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("운송장 등록 중 오류", e);
            throw new RuntimeException("운송장 등록 중 서버 오류가 발생했습니다", e);
        }
    }

    /* =========================================================================
     *  주문 숨김 처리
     * =========================================================================
     */
    @Override
    @JpaTransactional
    public boolean hideOrderFromList(UserPrincipal userPrincipal, String orderNumber) {

        try {
            log.debug("주문 숨김 처리 - provider={}, orderNumber={}",
                    userPrincipal.providerId(), orderNumber);

            Sellers seller = findSellerByPrincipal(userPrincipal);

            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(orderNumber, seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            Orders order = shipment.getOrders();
            validateHideOrderCondition(order.getOrderStatus(), shipment);

            if (shipment.isHiddenBySeller()) {
                log.info("이미 숨김 처리된 주문 - {}", orderNumber);
                return false;
            }

            shipment.hideFromSellerList();
            shipmentRepository.save(shipment);

            log.info("숨김 처리 완료 - {}", orderNumber);
            return true;

        } catch (NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            log.warn("주문 숨김 실패 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 숨김 처리 중 오류", e);
            throw new RuntimeException("주문 숨김 처리 중 서버 오류가 발생했습니다", e);
        }
    }

    /* =========================================================================
     *  공통 & 유틸 메서드
     * =========================================================================
     */

    /* ---------- 판매자 조회 ---------- */
    private Sellers findSellerByPrincipal(UserPrincipal principal) {
        Users user = userRepository.findByProviderAndProviderId(
                        principal.provider(), principal.providerId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));

        return sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("판매자 권한이 없습니다"));
    }

    /* ---------- 페이징 검증 ---------- */
    private void validatePageable(Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("페이지 크기는 100을 초과할 수 없습니다");
        }
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다");
        }
    }

    /* ---------- 상태 전환 검증 ---------- */
    private void validateStatusTransition(OrderStatusUpdateRequest req, OrderStatus current) {
        if (!req.isValidStatusTransition(current)) {
            throw new IllegalArgumentException(
                    String.format("잘못된 상태 전환: %s -> %s", current, req.getNewStatus()));
        }
    }

    private void validateReasonRequirement(OrderStatusUpdateRequest req) {
        if (req.isReasonRequired() && (req.getReason() == null || req.getReason().trim().isEmpty())) {
            throw new IllegalArgumentException("해당 상태 변경에는 사유가 필수입니다");
        }
    }

    private void handleSpecialStatusChange(OrderStatusUpdateRequest req, Shipments shipment) {
        if (req.isDelayRequest()) {
            log.info("출고 지연 처리 - orderNumber={}, reason={}", req.getOrderNumber(), req.getReason());
        }
    }

    /* ---------- 운송장 등록 시 검증 ---------- */
    private void validateTrackingNumberRegistration(
            Orders order, Shipments shipment, TrackingNumberRegisterRequest req) {

        if (order.getOrderStatus() != OrderStatus.READY_FOR_SHIPMENT) {
            throw new IllegalStateException("배송 준비 완료 상태에서만 운송장을 등록할 수 있습니다");
        }
        if (shipment.isShipped()) {
            throw new IllegalStateException("이미 운송장이 등록된 주문입니다");
        }
    }

    private void validateTrackingNumberDuplication(TrackingNumberRegisterRequest req) {
        if (shipmentRepository.existsByCourierAndTrackingNumber(
                req.getCourierDisplayName(), req.getNormalizedTrackingNumber())) {
            throw new IllegalStateException("이미 등록된 운송장 번호입니다");
        }
    }

    private TrackingNumberRegisterResponse.ValidationResult validateTrackingNumberWithApi(
            TrackingNumberRegisterRequest req) {

        if (!req.shouldValidateWithApi()) {
            return TrackingNumberRegisterResponse.ValidationResult.skipped("API 검증 생략");
        }
        // TODO: 스마트택배 API 연동
        return TrackingNumberRegisterResponse.ValidationResult.success("운송장 번호가 유효합니다");
    }

    /* ---------- 검색 관련 ---------- */
    private void validateSearchConditions(String searchType, String keyword) {
        if (searchType == null || searchType.isBlank()) {
            throw new IllegalArgumentException("검색 타입은 필수입니다");
        }
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색 키워드는 필수입니다");
        }
        if (keyword.trim().length() < 2) {
            throw new IllegalArgumentException("검색 키워드는 2자 이상이어야 합니다");
        }
        if (!isValidSearchType(searchType)) {
            throw new IllegalArgumentException("지원하지 않는 검색 타입(orderNumber, recipientName)");
        }
    }

    private boolean isValidSearchType(String searchType) {
        return "orderNumber".equals(searchType) || "recipientName".equals(searchType);
    }

    private Page<Shipments> performSearch(String sellerId,
                                          String searchType,
                                          String keyword,
                                          Pageable pageable) {
        return switch (searchType) {
            case "orderNumber"  ->
                    shipmentRepository.findSellerOrdersByOrderNumberWithPaging(sellerId, keyword, pageable);
            case "recipientName" ->
                    shipmentRepository.findSellerOrdersByRecipientNameWithPaging(sellerId, keyword, pageable);
            default -> throw new IllegalArgumentException("지원하지 않는 검색 타입: " + searchType);
        };
    }

    /* ---------- 숨김 처리 검증 ---------- */
    private void validateHideOrderCondition(OrderStatus status, Shipments shipment) {
        if (status != OrderStatus.DELIVERED && status != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("배송 완료/취소된 주문만 숨길 수 있습니다");
        }
    }

    /* =========================================================================
     *  DTO 변환 메서드 (기존 로직 + 수정사항 반영)
     * =========================================================================
     */

    private SellerOrderDetailResponse buildSellerOrderDetailResponse(
            Shipments shipment, List<OrderItems> orderItems, String sellerId) {

        Orders order = shipment.getOrders();

        var shippingAddress  = buildShippingAddress(shipment);
        var sellerItems      = orderItems.stream()
                .filter(i -> i.getProducts().getSeller().getUserId().equals(sellerId))
                .map(this::convertToSellerOrderDetailItem)
                .toList();
        var orderSummary     = buildOrderSummary(sellerItems);
        var shipmentInfo     = buildShipmentInfo(shipment);
        var statusManagement = buildStatusManagement(order.getOrderStatus(), shipment);

        return SellerOrderDetailResponse.of(
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getCreatedAt(),
                shippingAddress,
                sellerItems,
                orderSummary,
                shipmentInfo,
                statusManagement
        );
    }

    private SellerOrderListResponse.SellerOrderSummary convertToSellerOrderSummary(Shipments shipment) {

        Orders order = shipment.getOrders();

        List<OrderItems> sellerOrderItems = order.getOrderItems();   // 이미 Repository 단에서 판매자별 필터링

        List<SellerOrderListResponse.SellerOrderItem> itemDtos =
                sellerOrderItems.stream()
                        .map(this::convertToSellerOrderItem)
                        .toList();

        long totalPrice = sellerOrderItems.stream()
                .mapToLong(i -> i.getUnitPrice() * i.getQuantity())
                .sum();

        boolean canChangeStatus  = canChangeOrderStatus(order.getOrderStatus());
        boolean requiresTracking = order.getOrderStatus() == OrderStatus.READY_FOR_SHIPMENT &&
                !shipment.isShipped();
        String nextAction        = generateNextAction(order.getOrderStatus(), shipment);

        return SellerOrderListResponse.SellerOrderSummary.builder()
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getOrderStatus())
                .orderDate(order.getCreatedAt())
                .recipientName(shipment.getRecipientName())
                .maskedPhone(shipment.getMaskedPhone())
                .basicAddress(buildBasicAddress(shipment))
                .orderItems(itemDtos)
                .totalPrice(totalPrice)
                .itemCount(itemDtos.size())
                .courier(shipment.getCourier())
                .trackingNumber(shipment.getTrackingNumber())
                .shippedAt(shipment.getShippedAt())
                .deliveredAt(shipment.getDeliveredAt())
                .canChangeStatus(canChangeStatus)
                .requiresTracking(requiresTracking)
                .nextAction(nextAction)
                .build();
    }

    /* ---------- DTO‑Building Helper ---------- */

    private SellerOrderDetailResponse.ShippingAddress buildShippingAddress(Shipments s) {
        return SellerOrderDetailResponse.ShippingAddress.builder()
                .recipientName(s.getRecipientName())
                .recipientPhone(s.getRecipientPhone())
                .maskedPhone(s.getMaskedPhone())
                .zipCode(s.getZipCode())
                .address(s.getAddress())
                .addressDetail(s.getAddressDetail())
                .fullAddress(buildFullAddress(s))
                .deliveryRequest(s.getDeliveryRequest())
                .build();
    }

    private SellerOrderDetailResponse.SellerOrderDetailItem convertToSellerOrderDetailItem(OrderItems oi) {
        Products p = oi.getProducts();
        return SellerOrderDetailResponse.SellerOrderDetailItem.builder()
                .productId(p.getId())
                .productName(p.getProductName())
                .productImageUrl(p.getImageUrl())
                .productDescription(p.getDescription())
                .unitPrice(oi.getUnitPrice())
                .quantity(oi.getQuantity())
                .totalPrice(oi.getUnitPrice() * oi.getQuantity())
                .productOptions(oi.getProductOptions())
                .productSku(p.getSku())
                .categoryName(p.getCategory() != null ? p.getCategory().getCategoryName() : null)
                .build();
    }

    private SellerOrderDetailResponse.OrderSummary buildOrderSummary(
            List<SellerOrderDetailResponse.SellerOrderDetailItem> items) {

        long products = items.stream().mapToLong(SellerOrderDetailResponse.SellerOrderDetailItem::totalPrice).sum();
        long delivery = 0L;
        long discount = 0L;
        long finalAmt = products + delivery - discount;

        int itemCnt   = items.size();
        int qty       = items.stream().mapToInt(SellerOrderDetailResponse.SellerOrderDetailItem::quantity).sum();

        return SellerOrderDetailResponse.OrderSummary.builder()
                .totalProductPrice(products)
                .deliveryFee(delivery)
                .discountAmount(discount)
                .finalAmount(finalAmt)
                .totalItemCount(itemCnt)
                .totalQuantity(qty)
                .build();
    }

    private SellerOrderDetailResponse.ShipmentInfo buildShipmentInfo(Shipments s) {

        String trackingUrl = null;
        if (s.getCourier() != null && s.getTrackingNumber() != null) {
            CourierCompany cc = CourierCompany.fromDisplayName(s.getCourier());
            if (cc != null) trackingUrl = cc.generateTrackingUrl(s.getTrackingNumber());
        }

        return SellerOrderDetailResponse.ShipmentInfo.builder()
                .courier(s.getCourier())
                .trackingNumber(s.getTrackingNumber())
                .shippedAt(s.getShippedAt())
                .deliveredAt(s.getDeliveredAt())
                .trackingUpdatedAt(s.getTrackingUpdatedAt())
                .trackingUrl(trackingUrl)
                .shipmentMemo(null)
                .build();
    }

    private SellerOrderDetailResponse.StatusManagement buildStatusManagement(OrderStatus status, Shipments s) {

        var available = getAvailableStatusTransitions(status);
        boolean canChangeStatus = canChangeOrderStatus(status);

        boolean requiresTracking     = status == OrderStatus.READY_FOR_SHIPMENT && !s.isShipped();
        boolean canRegisterTracking  = requiresTracking;
        boolean canHideOrder         = status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED;
        String nextAction            = generateNextAction(status, s);
        String statusDesc            = getStatusDescription(status);

        return SellerOrderDetailResponse.StatusManagement.builder()
                .canChangeStatus(canChangeStatus)
                .availableStatuses(available)
                .requiresTracking(requiresTracking)
                .canRegisterTracking(canRegisterTracking)
                .canHideOrder(canHideOrder)
                .nextAction(nextAction)
                .statusDescription(statusDesc)
                .build();
    }

    private SellerOrderListResponse.SellerOrderItem convertToSellerOrderItem(OrderItems oi) {
        Products p = oi.getProducts();
        return SellerOrderListResponse.SellerOrderItem.builder()
                .productId(p.getId())
                .productName(p.getProductName())
                .productImageUrl(p.getImageUrl())
                .unitPrice(oi.getUnitPrice())
                .quantity(oi.getQuantity())
                .totalPrice(oi.getUnitPrice() * oi.getQuantity())
                .productOptions(oi.getProductOptions())
                .build();
    }

    /* ---------- 주소 Helper ---------- */
    private String buildBasicAddress(Shipments s) {
        return s.getAddress() == null ? "" : s.getAddress();
    }

    private String buildFullAddress(Shipments s) {
        StringBuilder sb = new StringBuilder();
        if (s.getZipCode() != null) sb.append("(").append(s.getZipCode()).append(") ");
        if (s.getAddress() != null) sb.append(s.getAddress());
        if (s.getAddressDetail() != null) sb.append(" ").append(s.getAddressDetail());
        return sb.toString().trim();
    }

    /* ---------- 상태/다음 액션 설명 ---------- */
    private boolean canChangeOrderStatus(OrderStatus s) {
        return switch (s) {
            case PAYMENT_COMPLETED, PREPARING, READY_FOR_SHIPMENT -> true;
            default -> false;
        };
    }

    private List<OrderStatus> getAvailableStatusTransitions(OrderStatus s) {
        return switch (s) {
            case PAYMENT_COMPLETED -> List.of(OrderStatus.PREPARING, OrderStatus.CANCELLED);
            case PREPARING        -> List.of(OrderStatus.READY_FOR_SHIPMENT, OrderStatus.CANCELLED);
            case READY_FOR_SHIPMENT -> List.of(OrderStatus.IN_DELIVERY);
            default -> List.of();
        };
    }

    private String generateNextAction(OrderStatus s, Shipments sh) {
        return switch (s) {
            case PAYMENT_COMPLETED -> "상품 준비 후 '상품준비중'으로 변경하세요";
            case PREPARING        -> "포장 완료 후 '배송준비완료'로 변경하세요";
            case READY_FOR_SHIPMENT -> sh.isShipped() ?
                    "배송 중입니다" : "운송장 등록 후 배송을 시작하세요";
            case IN_DELIVERY      -> "배송 완료 시 자동 변경됩니다";
            case DELIVERED        -> "배송이 완료되었습니다";
            case CANCELLED        -> "주문이 취소되었습니다";
            default               -> "추가 작업 없음";
        };
    }

    private String getStatusDescription(OrderStatus s) {
        return switch (s) {
            case PAYMENT_COMPLETED -> "결제가 완료되었습니다";
            case PREPARING        -> "상품 준비 중입니다";
            case READY_FOR_SHIPMENT -> "배송 준비가 완료되었습니다";
            case IN_DELIVERY      -> "배송 중입니다";
            case DELIVERED        -> "배송이 완료되었습니다";
            case CANCELLED        -> "주문이 취소되었습니다";
            default               -> "상태 확인 필요";
        };
    }
}
