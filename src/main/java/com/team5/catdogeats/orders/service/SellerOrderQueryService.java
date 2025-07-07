package com.team5.catdogeats.orders.service.seller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.CourierCompany;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 판매자용 주문 상세 조회 서비스
 * 단일 책임: 주문 상세 정보 조회 및 DTO 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderQueryService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final ShipmentRepository shipmentRepository;

    /**
     * 판매자용 주문 상세 조회
     * @param userPrincipal 인증된 판매자 정보
     * @param orderNumber 주문 번호
     * @return 주문 상세 정보
     */
    @JpaTransactional(readOnly = true)
    public SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, String orderNumber) {
        try {
            log.debug("판매자 주문 상세 조회 시작 - provider={}, providerId={}, orderNumber={}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 검증
            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(orderNumber, seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            // 3. 해당 판매자 상품 목록 필터링
            List<OrderItems> sellerOrderItems = shipment.getOrders().getOrderItems().stream()
                    .filter(item -> item.getProducts().getSeller().getUserId().equals(seller.getUserId()))
                    .toList();

            // 4. DTO 변환 및 반환
            return buildSellerOrderDetailResponse(shipment, sellerOrderItems, seller.getUserId());

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("주문 상세 조회 실패 - orderNumber={}, reason={}", orderNumber, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 상세 조회 중 오류 - orderNumber={}", orderNumber, e);
            throw new RuntimeException("주문 상세 조회 중 서버 오류가 발생했습니다", e);
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

    private SellerOrderDetailResponse buildSellerOrderDetailResponse(
            Shipments shipment, List<OrderItems> orderItems, String sellerId) {

        Orders order = shipment.getOrders();

        // 배송지 정보 생성
        SellerOrderDetailResponse.ShippingAddress shippingAddress =
                buildShippingAddress(shipment);

        // 주문 상품 정보 변환
        List<SellerOrderDetailResponse.SellerOrderDetailItem> sellerItems =
                orderItems.stream()
                        .map(this::convertToSellerOrderDetailItem)
                        .toList();

        // 주문 요약 정보 생성
        SellerOrderDetailResponse.OrderSummary orderSummary =
                buildOrderSummary(sellerItems);

        // 배송 정보 생성
        SellerOrderDetailResponse.ShipmentInfo shipmentInfo =
                buildShipmentInfo(shipment);

        // 상태 관리 정보 생성
        SellerOrderDetailResponse.StatusManagement statusManagement =
                buildStatusManagement(order.getOrderStatus(), shipment);

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

    private SellerOrderDetailResponse.ShippingAddress buildShippingAddress(Shipments shipment) {
        return SellerOrderDetailResponse.ShippingAddress.builder()
                .recipientName(shipment.getRecipientName())
                .recipientPhone(shipment.getRecipientPhone())
                .maskedPhone(shipment.getMaskedPhone())
                .zipCode(shipment.getZipCode())
                .address(shipment.getAddress())
                .addressDetail(shipment.getAddressDetail())
                .fullAddress(buildFullAddress(shipment))
                .deliveryRequest(shipment.getDeliveryRequest())
                .build();
    }

    private SellerOrderDetailResponse.SellerOrderDetailItem convertToSellerOrderDetailItem(OrderItems orderItem) {
        Products product = orderItem.getProducts();
        return SellerOrderDetailResponse.SellerOrderDetailItem.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .productImageUrl(product.getImageUrl())
                .productDescription(product.getDescription())
                .unitPrice(orderItem.getUnitPrice())
                .quantity(orderItem.getQuantity())
                .totalPrice(orderItem.getUnitPrice() * orderItem.getQuantity())
                .productOptions(orderItem.getProductOptions())
                .productSku(product.getSku())
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .build();
    }

    private SellerOrderDetailResponse.OrderSummary buildOrderSummary(
            List<SellerOrderDetailResponse.SellerOrderDetailItem> items) {

        Long totalProductPrice = items.stream()
                .mapToLong(SellerOrderDetailResponse.SellerOrderDetailItem::totalPrice)
                .sum();

        int totalItemCount = items.size();
        int totalQuantity = items.stream()
                .mapToInt(SellerOrderDetailResponse.SellerOrderDetailItem::quantity)
                .sum();

        // 배송비는 판매자별로 별도 계산 (현재는 0으로 설정)
        Long deliveryFee = 0L;
        Long discountAmount = 0L;
        Long finalAmount = totalProductPrice + deliveryFee - discountAmount;

        return SellerOrderDetailResponse.OrderSummary.builder()
                .totalProductPrice(totalProductPrice)
                .deliveryFee(deliveryFee)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .totalItemCount(totalItemCount)
                .totalQuantity(totalQuantity)
                .build();
    }

    private SellerOrderDetailResponse.ShipmentInfo buildShipmentInfo(Shipments shipment) {
        String trackingUrl = null;
        if (shipment.getCourier() != null && shipment.getTrackingNumber() != null) {
            CourierCompany courier = CourierCompany.fromDisplayName(shipment.getCourier());
            if (courier != null) {
                trackingUrl = courier.generateTrackingUrl(shipment.getTrackingNumber());
            }
        }

        return SellerOrderDetailResponse.ShipmentInfo.builder()
                .courier(shipment.getCourier())
                .trackingNumber(shipment.getTrackingNumber())
                .shippedAt(shipment.getShippedAt())
                .deliveredAt(shipment.getDeliveredAt())
                .trackingUpdatedAt(shipment.getTrackingUpdatedAt())
                .trackingUrl(trackingUrl)
                .shipmentMemo(shipment.getShipmentMemo())
                .build();
    }

    private SellerOrderDetailResponse.StatusManagement buildStatusManagement(
            OrderStatus currentStatus, Shipments shipment) {

        List<OrderStatus> availableStatuses = getAvailableStatusTransitions(currentStatus);
        boolean canChangeStatus = canChangeOrderStatus(currentStatus);
        boolean requiresTracking = currentStatus == OrderStatus.READY_FOR_SHIPMENT && !shipment.isShipped();
        boolean canRegisterTracking = requiresTracking;
        boolean canHideOrder = currentStatus == OrderStatus.DELIVERED || currentStatus == OrderStatus.CANCELLED;
        String nextAction = generateNextAction(currentStatus, shipment);
        String statusDescription = getStatusDescription(currentStatus);

        return SellerOrderDetailResponse.StatusManagement.builder()
                .canChangeStatus(canChangeStatus)
                .availableStatuses(availableStatuses)
                .requiresTracking(requiresTracking)
                .canRegisterTracking(canRegisterTracking)
                .canHideOrder(canHideOrder)
                .nextAction(nextAction)
                .statusDescription(statusDescription)
                .build();
    }

    private String buildFullAddress(Shipments shipment) {
        StringBuilder fullAddress = new StringBuilder();
        if (shipment.getZipCode() != null) {
            fullAddress.append("(").append(shipment.getZipCode()).append(") ");
        }
        if (shipment.getAddress() != null) {
            fullAddress.append(shipment.getAddress());
        }
        if (shipment.getAddressDetail() != null) {
            fullAddress.append(" ").append(shipment.getAddressDetail());
        }
        return fullAddress.toString().trim();
    }

    private boolean canChangeOrderStatus(OrderStatus status) {
        return switch (status) {
            case PAYMENT_COMPLETED, PREPARING, READY_FOR_SHIPMENT -> true;
            case IN_DELIVERY, DELIVERED, CANCELLED, REFUND_PROCESSING, REFUNDED -> false;
            default -> false;
        };
    }

    private List<OrderStatus> getAvailableStatusTransitions(OrderStatus currentStatus) {
        return switch (currentStatus) {
            case PAYMENT_COMPLETED -> List.of(OrderStatus.PREPARING, OrderStatus.CANCELLED);
            case PREPARING -> List.of(OrderStatus.READY_FOR_SHIPMENT, OrderStatus.CANCELLED);
            case READY_FOR_SHIPMENT -> List.of(OrderStatus.IN_DELIVERY);
            default -> List.of();
        };
    }

    private String generateNextAction(OrderStatus currentStatus, Shipments shipment) {
        return switch (currentStatus) {
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

    private String getStatusDescription(OrderStatus status) {
        return switch (status) {
            case PAYMENT_COMPLETED -> "결제가 완료되어 주문이 확정되었습니다";
            case PREPARING -> "상품을 준비하고 있습니다";
            case READY_FOR_SHIPMENT -> "배송 준비가 완료되었습니다";
            case IN_DELIVERY -> "상품이 배송 중입니다";
            case DELIVERED -> "배송이 완료되었습니다";
            case CANCELLED -> "주문이 취소되었습니다";
            default -> "주문 상태를 확인해주세요";
        };
    }
}