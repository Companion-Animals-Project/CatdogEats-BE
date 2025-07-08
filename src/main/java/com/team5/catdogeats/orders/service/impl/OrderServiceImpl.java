package com.team5.catdogeats.orders.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.OrderPendingDetails;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.dto.response.OrderDeleteResponse;
import com.team5.catdogeats.orders.dto.response.OrderDetailResponse;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderPendingDetailsRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.OrderService;
import com.team5.catdogeats.orders.util.ShippingAddressUtils;
import com.team5.catdogeats.orders.util.TossPaymentResponseBuilder;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderPendingDetailsRepository orderPendingDetailsRepository;
    private final UserRepository userRepository;
    private final BuyerRepository buyerRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TossPaymentResponseBuilder tossPaymentResponseBuilder;
    private final ShipmentRepository shipmentRepository;
    private final ObjectMapper objectMapper;

    @Override
    @JpaTransactional
    public OrderCreateResponse createOrderByUserPrincipal(UserPrincipal userPrincipal, OrderCreateRequest request) {
        log.info("주문 생성 요청: provider={}, providerId={}, itemCount={}, 쿠폰할인={}",
                userPrincipal.provider(), userPrincipal.providerId(),
                request.getOrderItems().size(),
                getCouponDescription(request.getPaymentInfo())); // 로그 메시지 개선

        try {
            // 1. 구매자 검증
            Users user = findUserByPrincipal(userPrincipal);

            // 2. 주문 상품들 검증 및 정보 수집 (원가 기준)
            List<OrderItemInfo> validatedOrderItems = validateAndCollectOrderItems(request.getOrderItems());

            // 3. 쿠폰 할인 검증 (추가)
            validateCouponRequest(request.getPaymentInfo());

            // 4. 주문 금액 계산 (메서드 시그니처 변경)
            Long originalTotalPrice = calculateOriginalTotalPrice(validatedOrderItems);
            Long discountedTotalPrice = applyCouponDiscount(originalTotalPrice, request.getPaymentInfo());
            Long finalPaymentAmount = calculateFinalPaymentAmount(discountedTotalPrice);

            log.debug("주문 금액 계산: 원가={}원, 할인후={}원, 최종={}원",
                    originalTotalPrice, discountedTotalPrice, finalPaymentAmount);

            // 5. Orders 엔티티만 생성 및 저장 (PAYMENT_PENDING 상태)
            Orders savedOrder = createAndSaveOrderOnly(user, finalPaymentAmount);

            // 6. OrderPendingDetails에 임시 정보 저장 (메서드 시그니처 변경)
            saveOrderPendingDetails(savedOrder, userPrincipal, originalTotalPrice,
                    request.getPaymentInfo(), validatedOrderItems, request.getShippingAddress());

            log.info("주문 생성 완료: orderId={}, orderNumber={}, status={}, 최종금액={}원",
                    savedOrder.getId(), savedOrder.getOrderNumber(), savedOrder.getOrderStatus(), finalPaymentAmount);

            // 6. OrderCreatedEvent 발행 (재고 예약 및 결제 정보 생성용)
            OrderCreatedEvent event = OrderCreatedEvent.of(
                    savedOrder.getId(),
                    savedOrder.getOrderNumber(),
                    user.getId(),
                    userPrincipal.provider(),
                    userPrincipal.providerId(),
                    finalPaymentAmount,
                    validatedOrderItems
            );

            eventPublisher.publishEvent(event);
            log.debug("OrderCreatedEvent 발행 완료: orderId={} (shippingAddress는 OrderPendingDetails에 저장됨)",
                    savedOrder.getId());

            // 7. Toss Payments 응답 생성
            String orderName = validatedOrderItems.get(0).productName() +
                    (validatedOrderItems.size() > 1 ? String.format(" 외 %d개", validatedOrderItems.size() - 1) : "");

            return tossPaymentResponseBuilder.buildTossPaymentResponse(
                    savedOrder,
                    request.getPaymentInfo(),
                    orderName
            );

        } catch (Exception e) {
            log.error("주문 생성 실패: error={}", e.getMessage(), e);
            throw e instanceof RuntimeException ? (RuntimeException) e :
                    new RuntimeException("주문 생성 중 서버 오류가 발생했습니다", e);
        }
    }

    /**
     * 쿠폰 할인 요청 검증
     */
    private void validateCouponRequest(OrderCreateRequest.PaymentInfoRequest paymentInfo) {
        if (paymentInfo != null) {
            paymentInfo.validateCouponConsistency();
        }
    }

    /**
     * 쿠폰 설명 문자열 생성 (로그용)
     */
    private String getCouponDescription(OrderCreateRequest.PaymentInfoRequest paymentInfo) {
        if (paymentInfo == null || !paymentInfo.isCouponApplied()) {
            return "없음";
        }

        if (paymentInfo.getCouponType() == null) {
            // 기존 방식
            return paymentInfo.getCouponDiscountRate() + "%";
        }

        return switch (paymentInfo.getCouponType()) {
            case PERCENT -> paymentInfo.getCouponDiscountRate() + "%";
            case AMOUNT -> String.format("%,d원", paymentInfo.getCouponDiscountAmount());
        };
    }

    /**
     * 쿠폰 할인 적용 (새 버전 - PaymentInfoRequest 전체를 받음)
     */
    private Long applyCouponDiscount(Long originalTotalPrice, OrderCreateRequest.PaymentInfoRequest paymentInfo) {
        if (paymentInfo == null || !paymentInfo.isCouponApplied()) {
            return originalTotalPrice;
        }

        if (paymentInfo.getCouponType() == null) {
            // 기존 방식 (하위 호환성)
            return applyCouponDiscountLegacy(originalTotalPrice, paymentInfo.getCouponDiscountRate());
        }

        return switch (paymentInfo.getCouponType()) {
            case PERCENT -> applyCouponDiscountPercent(originalTotalPrice, paymentInfo.getCouponDiscountRate());
            case AMOUNT -> applyCouponDiscountAmount(originalTotalPrice, paymentInfo.getCouponDiscountAmount());
        };
    }

    private Users findUserByPrincipal(UserPrincipal userPrincipal) {
        BuyerDTO buyer = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("구매자를 찾을 수 없거나 권한이 없습니다"));
        return userRepository.getReferenceById(buyer.userId());
    }

    private List<OrderItemInfo> validateAndCollectOrderItems(List<OrderCreateRequest.OrderItemRequest> orderItems) {
        List<OrderItemInfo> validatedItems = new ArrayList<>();
        for (OrderCreateRequest.OrderItemRequest item : orderItems) {
            Products product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + item.getProductId()));

            if (item.getQuantity() <= 0) {
                throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다");
            }

            validatedItems.add(new OrderItemInfo(
                    product.getId(),
                    product.getTitle(),
                    item.getQuantity(),
                    product.getPrice(),
                    product.getPrice() * item.getQuantity()
            ));
        }
        return validatedItems;
    }

    private Long calculateOriginalTotalPrice(List<OrderItemInfo> orderItems) {
        return orderItems.stream()
                .mapToLong(item -> item.unitPrice() * item.quantity())
                .sum();
    }

    private Long applyCouponDiscountLegacy(Long originalTotalPrice, Double couponDiscountRate) {
        if (couponDiscountRate == null || couponDiscountRate <= 0) {
            return originalTotalPrice;
        }
        if (couponDiscountRate > 100) {
            throw new IllegalArgumentException("쿠폰 할인율은 100%를 초과할 수 없습니다.");
        }
        return Math.round(originalTotalPrice * (1 - couponDiscountRate / 100.0));
    }

    /**
     * 정률 할인 적용
     */
    private Long applyCouponDiscountPercent(Long originalTotalPrice, Double couponDiscountRate) {
        if (couponDiscountRate == null || couponDiscountRate <= 0) {
            return originalTotalPrice;
        }
        if (couponDiscountRate > 100) {
            throw new IllegalArgumentException("쿠폰 할인율은 100%를 초과할 수 없습니다.");
        }

        Long discountedAmount = Math.round(originalTotalPrice * (1 - couponDiscountRate / 100.0));
        log.debug("정률 할인 적용: {}% → {}원 할인 ({}원 → {}원)",
                couponDiscountRate, originalTotalPrice - discountedAmount, originalTotalPrice, discountedAmount);

        return discountedAmount;
    }

    /**
     * 정액 할인 적용
     */
    private Long applyCouponDiscountAmount(Long originalTotalPrice, Long couponDiscountAmount) {
        if (couponDiscountAmount == null || couponDiscountAmount <= 0) {
            return originalTotalPrice;
        }
        if (couponDiscountAmount > originalTotalPrice) {
            throw new IllegalArgumentException("쿠폰 할인 금액이 주문 총액을 초과할 수 없습니다.");
        }

        Long discountedAmount = originalTotalPrice - couponDiscountAmount;
        log.debug("정액 할인 적용: {}원 할인 ({}원 → {}원)",
                couponDiscountAmount, originalTotalPrice, discountedAmount);

        return discountedAmount;
    }

    private Long calculateFinalPaymentAmount(Long discountedTotalPrice) {
        long deliveryFee = discountedTotalPrice >= 50000 ? 0 : 3000;
        long finalAmount = discountedTotalPrice + deliveryFee;
        // 100% 할인 등으로 최종 결제 금액이 0원이 될 경우, 최소 결제 금액 1원으로 보정
        return Math.max(finalAmount, 1L);
    }

    private Orders createAndSaveOrderOnly(Users user, Long finalPaymentAmount) {
        Orders order = Orders.builder()
                .user(user)
                .orderNumber(generateOrderNumber()) // String 타입 반환값 사용
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .totalPrice(finalPaymentAmount)
                .build();
        return orderRepository.save(order);
    }

    private void saveOrderPendingDetails(Orders order, UserPrincipal userPrincipal,
                                         Long originalTotalPrice,
                                         OrderCreateRequest.PaymentInfoRequest paymentInfo,
                                         List<OrderItemInfo> orderItems,
                                         OrderCreateRequest.ShippingAddressRequest shippingAddress) {
        try {
            String orderItemsJson = objectMapper.writeValueAsString(orderItems);
            String shippingAddressJson = objectMapper.writeValueAsString(shippingAddress);

            OrderPendingDetails.OrderPendingDetailsBuilder builder = OrderPendingDetails.builder()
                    .orders(order)
                    .userProvider(userPrincipal.provider())
                    .userProviderId(userPrincipal.providerId())
                    .originalTotalPrice(originalTotalPrice)
                    .orderItemsJson(orderItemsJson)
                    .shippingAddressJson(shippingAddressJson);

            // 쿠폰 정보 설정
            if (paymentInfo != null && paymentInfo.isCouponApplied()) {
                builder.couponType(paymentInfo.getCouponType())
                        .couponDiscountRate(paymentInfo.getCouponDiscountRate())
                        .couponDiscountAmount(paymentInfo.getCouponDiscountAmount());
            }

            orderPendingDetailsRepository.save(builder.build());

        } catch (JsonProcessingException e) {
            log.error("OrderPendingDetails JSON 직렬화 실패: orderId={}, error={}", order.getId(), e.getMessage());
            throw new RuntimeException("주문 정보 저장 중 오류가 발생했습니다", e);
        }
    }

    @Override
    @JpaTransactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(UserPrincipal userPrincipal, String orderNumber) {
        log.info("주문 상세 조회 시작 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        Users user = findUserByPrincipal(userPrincipal);
        Orders order = orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다."));

        if (order.getOrderStatus() == OrderStatus.PAYMENT_PENDING) {
            log.warn("결제가 완료되지 않은 주문에 대한 상세 조회 시도: orderNumber={}", orderNumber);
            throw new IllegalStateException("결제가 완료되지 않은 주문은 조회할 수 없습니다.");
        }

        return getOrderDetailFromEntities(order);
    }

    private OrderDetailResponse getOrderDetailFromEntities(Orders order) {
        Shipments shipment = shipmentRepository.findByOrders(order).orElse(null);

        List<OrderDetailResponse.OrderItemDetail> orderItemDetails = order.getOrderItems().stream()
                .map(orderItem -> new OrderDetailResponse.OrderItemDetail(
                        orderItem.getId(),
                        orderItem.getProducts().getId(),
                        orderItem.getProducts().getTitle(),
                        orderItem.getQuantity(),
                        orderItem.getPrice(),
                        orderItem.getPrice() * orderItem.getQuantity()
                )).toList();

        Long totalProductPrice = orderItemDetails.stream()
                .mapToLong(OrderDetailResponse.OrderItemDetail::totalPrice)
                .sum();

        Long discountAmount = calculateDiscountAmount(order, totalProductPrice);
        Long deliveryFee = calculateDeliveryFee(totalProductPrice);

        OrderDetailResponse.RecipientInfo recipientInfo = createRecipientInfoFromShipment(shipment);
        OrderDetailResponse.PaymentInfo paymentInfo = OrderDetailResponse.PaymentInfo.of(
                totalProductPrice, discountAmount, deliveryFee);

        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCreatedAt(),
                order.getOrderStatus(),
                recipientInfo,
                paymentInfo,
                orderItemDetails
        );
    }

    @Override
    @JpaTransactional
    public OrderDeleteResponse deleteOrder(UserPrincipal userPrincipal, String orderNumber) {
        log.info("주문 내역 삭제 요청 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        try {
            Users user = findUserByPrincipal(userPrincipal);
            Orders order = orderRepository.findOrderDetailByUserAndOrderNumber(user, orderNumber)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            if (isDeletionRestricted(order.getOrderStatus())) {
                throw new IllegalArgumentException(getDeletionRestrictionMessage(order.getOrderStatus()));
            }

            if (order.isOrderHidden()) {
                throw new IllegalArgumentException("이미 삭제된 주문 내역입니다");
            }

            order.hideOrder();
            Orders savedOrder = orderRepository.save(order);

            return OrderDeleteResponse.success(
                    savedOrder.getOrderNumber(),
                    savedOrder.getId(),
                    savedOrder.getHiddenAt()
            );

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("주문 내역 삭제 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            return OrderDeleteResponse.failure(orderNumber, e.getMessage());

        } catch (Exception e) {
            log.error("주문 내역 삭제 중 예상치 못한 오류 발생 - orderNumber: {}", orderNumber, e);
            return OrderDeleteResponse.failure(orderNumber, "주문 내역 삭제 중 서버 오류가 발생했습니다");
        }
    }

    private boolean isDeletionRestricted(OrderStatus orderStatus) {
        return List.of(
                OrderStatus.PAYMENT_COMPLETED,
                OrderStatus.PREPARING,
                OrderStatus.READY_FOR_SHIPMENT,
                OrderStatus.IN_DELIVERY,
                OrderStatus.REFUND_PROCESSING
        ).contains(orderStatus);
    }

    private String getDeletionRestrictionMessage(OrderStatus orderStatus) {
        return switch (orderStatus) {
            case PAYMENT_COMPLETED -> "결제가 완료된 주문은 삭제할 수 없습니다. 상품 준비 진행상황을 확인해주세요.";
            case PREPARING -> "상품 준비 중인 주문은 삭제할 수 없습니다.";
            case READY_FOR_SHIPMENT -> "배송 준비가 완료된 주문은 삭제할 수 없습니다.";
            case IN_DELIVERY -> "배송 중인 주문은 삭제할 수 없습니다. 배송 조회 페이지에서 배송상황을 확인해주세요.";
            case REFUND_PROCESSING -> "환불 처리 중인 주문은 삭제할 수 없습니다. 환불 진행상황을 확인해주세요.";
            default -> "현재 상태에서는 주문을 삭제할 수 없습니다.";
        };
    }

    private Long calculateDiscountAmount(Orders order, Long totalProductPrice) {
        Long deliveryFee = calculateDeliveryFee(totalProductPrice);
        Long discountedAmount = order.getTotalPrice() - deliveryFee;
        return Math.max(0L, totalProductPrice - discountedAmount);
    }

    private Long calculateDeliveryFee(Long totalProductPrice) {
        return totalProductPrice >= 50000L ? 0L : 3000L;
    }

    private OrderDetailResponse.RecipientInfo createRecipientInfoFromShipment(Shipments shipment) {
        if (shipment != null) {
            return new OrderDetailResponse.RecipientInfo(
                    shipment.getRecipientName(),
                    shipment.getRecipientPhone(),
                    ShippingAddressUtils.buildFullAddress(shipment.getStreetAddress(), shipment.getDetailAddress()),
                    shipment.getDeliveryRequest() != null ? shipment.getDeliveryRequest() : "배송 요청사항 없음"
            );
        }
        return new OrderDetailResponse.RecipientInfo("수령인 미등록", "연락처 미등록", "주소 미등록", "배송 요청사항 없음");
    }

    private String generateOrderNumber() {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomSuffix = ThreadLocalRandom.current().nextInt(10000, 100000);
        return timestamp + randomSuffix;
    }
}