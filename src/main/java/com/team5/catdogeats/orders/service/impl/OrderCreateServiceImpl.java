package com.team5.catdogeats.orders.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.repository.BuyerCouponRepository;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.OrderPendingDetails;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.GroupSellerAndCouponsDTO;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.dto.common.OrderItemSnapshot;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.dto.response.OrderDeleteResponse;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderPendingDetailsRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.OrderCreateService;
import com.team5.catdogeats.orders.util.TossPaymentResponseBuilder;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateServiceImpl implements OrderCreateService {

    private final SellersRepository sellersRepository;
    private final OrderRepository orderRepository;
    private final OrderPendingDetailsRepository orderPendingDetailsRepository;
    private final BuyerRepository buyerRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TossPaymentResponseBuilder tossPaymentResponseBuilder;
    private final ObjectMapper objectMapper;
    private final BuyerCouponRepository buyerCouponRepository;

    @Override
    @JpaTransactional
    public OrderCreateResponse createOrderByUserPrincipal(UserPrincipal userPrincipal, OrderCreateRequest request) {
        log.debug("주문 생성 요청: provider={}, providerId={}, itemCount={}",
                userPrincipal.provider(), userPrincipal.providerId(),
                request.getOrderItems().size()); // 로그 메시지 개선

        try {
            // 1. 구매자 검증
            Buyers buyers = findUserByPrincipal(userPrincipal);

            // 2. 주문 상품들 검증 및 정보 수집 (원가 기준)
            Map<Sellers, List<OrderItemInfo>> validatedOrderItems = validateAndCollectOrderItems(request.getOrderItems());

            // 3. 쿠폰 할인 검증 (추가)
            List<GroupSellerAndCouponsDTO> coupons = getCouponRequest(buyers, request.getPaymentInfo());

            // 4. 주문 금액 계산 (메서드 시그니처 변경)
            Long originalTotalPrice = calculateOriginalTotalPrice(validatedOrderItems);
            Long discountAmount = applyCouponDiscount(coupons, validatedOrderItems);

            Long totalDeliveryFee = calculateTotalDeliveryFee(validatedOrderItems)  ;
            Long discountedTotalPrice = originalTotalPrice - discountAmount;
            Long finalPaymentAmount = calculateFinalPaymentAmount(discountedTotalPrice, totalDeliveryFee);

            log.debug("주문 금액 계산: 원가={}원, 할인후={}원, 최종={}원",
                    originalTotalPrice, discountedTotalPrice, finalPaymentAmount);

            // 5. Orders 엔티티만 생성 및 저장 (PAYMENT_PENDING 상태)
            Orders savedOrder = createAndSaveOrderOnly(buyers, originalTotalPrice, discountAmount, finalPaymentAmount, totalDeliveryFee);

            // 6. OrderPendingDetails에 임시 정보 저장 (메서드 시그니처 변경)
            saveOrderPendingDetails(savedOrder, buyers, originalTotalPrice, totalDeliveryFee, finalPaymentAmount,
                    request.getPaymentInfo(), validatedOrderItems, request.getShippingAddress());

            log.debug("주문 생성 완료: orderId={}, orderNumber={}, status={}, 최종금액={}원",
                    savedOrder.getId(), savedOrder.getOrderNumber(), savedOrder.getOrderStatus(), finalPaymentAmount);


            List<OrderItemInfo> flatItems = validatedOrderItems.values().stream()
                    .flatMap(List::stream)
                    .toList();

            // 6. OrderCreatedEvent 발행 (재고 예약 및 결제 정보 생성용)
            OrderCreatedEvent event = OrderCreatedEvent.of(
                    savedOrder.getId(),
                    savedOrder.getOrderNumber(),
                    buyers.getUserId(),
                    userPrincipal.provider(),
                    userPrincipal.providerId(),
                    finalPaymentAmount,
                    flatItems
            );

            eventPublisher.publishEvent(event);
            log.debug("OrderCreatedEvent 발행 완료: orderId={} (shippingAddress는 OrderPendingDetails에 저장됨)",
                    savedOrder.getId());

            // 7. Toss Payments 응답 생성
            String orderName = validatedOrderItems.values().stream()
                    .flatMap(List::stream)
                    .map(OrderItemInfo::productName)
                    .limit(1)
                    .findFirst()
                    .map(firstName -> {
                        long totalCount = validatedOrderItems.values().stream()
                                .mapToLong(List::size)
                                .sum();
                        return totalCount > 1 ? firstName + " 외 " + (totalCount - 1) + "개" : firstName;
                    })
                    .orElse("주문 상품");


            return tossPaymentResponseBuilder.buildTossPaymentResponse(
                    savedOrder,
                    request.getPaymentInfo(),
                    orderName
            );

        } catch (NoSuchElementException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("주문 생성 실패: error={}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


    private long calculateTotalDeliveryFee(Map<Sellers, List<OrderItemInfo>> bySeller){
        return bySeller.entrySet().stream()
                .mapToLong(e -> {
                    Sellers s = e.getKey();
                    long sub = e.getValue().stream().mapToLong(OrderItemInfo::totalPrice).sum();
                    return sub >= s.getFreeShippingThreshold() ? 0L : s.getDeliveryFee();
                }).sum();
    }


    /**
     * 쿠폰 할인 요청 검증
     */
    private List<GroupSellerAndCouponsDTO> getCouponRequest(Buyers buyers, OrderCreateRequest.PaymentInfoRequest paymentInfo) {
        if (paymentInfo == null || paymentInfo.getSellerCoupons().isEmpty()) {
            return null;
        }

        List<String> ids = Optional.of(paymentInfo)
                .map(OrderCreateRequest.PaymentInfoRequest::getSellerCoupons)
                .orElse(List.of());
        List<String> distinctIds = ids.stream().distinct().toList();

        List<GroupSellerAndCouponsDTO> result = buyerCouponRepository.findValidCoupons(buyers, paymentInfo.getSellerCoupons(), LocalDate.now());

        if (result.size() != distinctIds.size()) {
            Set<String> ok = result.stream()
                    .map(dto -> dto.coupons().getId())
                    .collect(Collectors.toSet());

            String invalid = distinctIds.stream()
                    .filter(id -> !ok.contains(id))
                    .collect(Collectors.joining(", "));
            throw new NoSuchElementException("사용할 수 없는 쿠폰: " + invalid);
        }

        Map<Sellers, List<GroupSellerAndCouponsDTO>> bySeller =
                result.stream()
                        .collect(Collectors.groupingBy(GroupSellerAndCouponsDTO::sellers));

        bySeller.forEach((seller, list) -> {
            if (list.size() > 1) {
                throw new IllegalArgumentException(
                        "판매자(" + seller.getUserId() + ")당 쿠폰은 하나만 사용할 수 있습니다.");
            }
        });
        return result;
    }

    /**
     * 쿠폰 할인 적용 (새 버전 - PaymentInfoRequest 전체를 받음)
     */
    private long applyCouponDiscount(List<GroupSellerAndCouponsDTO> coupons,
                                     Map<Sellers, List<OrderItemInfo>> bySeller){
        if(coupons==null || coupons.isEmpty()) return 0L;

        return coupons.stream().mapToLong(c -> {
            Sellers seller = c.sellers();
            long sub = bySeller.getOrDefault(seller, List.of())
                    .stream().mapToLong(OrderItemInfo::totalPrice).sum();

            return switch (c.coupons().getDiscountType()){
                case PERCENT -> applyCouponDiscountPercent(sub, c.coupons().getDiscountValue());
                case AMOUNT  -> applyCouponDiscountAmount(sub, c.coupons().getDiscountValue());
            };
        }).sum();
    }



    private Buyers findUserByPrincipal(UserPrincipal userPrincipal) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("구매자를 찾을 수 없거나 권한이 없습니다"));
        return BuyerDTO.toEntity(buyerDTO);
    }

    private Map<Sellers, List<OrderItemInfo>> validateAndCollectOrderItems(
            List<OrderCreateRequest.OrderItemRequest> reqs){

        List<OrderItemInfo> rawItems =reqs.stream()
                .map(r -> {
                    if (r.getQuantity() <= 0)
                        throw new IllegalArgumentException("수량은 1개 이상");
                    Products p = productRepository.findById(r.getProductId())
                            .orElseThrow(() -> new NoSuchElementException("상품 없음: " + r.getProductId()));
                    return OrderItemInfo.of(p, r.getQuantity());
                })
                .toList();
        Map<String, OrderItemInfo> mergedByProduct = rawItems.stream()
                .collect(Collectors.toMap(
                        OrderItemInfo::productId,
                        i -> i,
                        OrderItemInfo::mergeQuantity         // 결합 함수를 사용해 합산
                ));

        // 판매자별로 그룹핑
        return mergedByProduct.values().stream()
                .collect(Collectors.groupingBy(OrderItemInfo::seller));
    }


    private Long calculateOriginalTotalPrice(Map<Sellers, List<OrderItemInfo>> groupedOrderItems) {
        return groupedOrderItems.values().stream()
                .flatMap(List::stream)
                .mapToLong(OrderItemInfo::totalPrice)
                .sum();
    }


    /**
     * 정률 할인 적용
     */
    private Long applyCouponDiscountPercent(Long subTotal, Integer couponDiscountRate) {
        if (couponDiscountRate <= 0 || couponDiscountRate > 100) {
            throw new IllegalArgumentException("할인율 0이하 또는 100을 초과할 수 없습니다.");
        }
        return Math.round(subTotal * (couponDiscountRate / 100.0));
    }

    private long applyCouponDiscountAmount(long subTotal, int amount) {
        if (amount <= 0 || amount > subTotal)
            throw new IllegalArgumentException("할인 금액 오류");
        return amount;
    }

    private Long calculateFinalPaymentAmount(Long discountedTotalPrice, Long totalDeliveryFee) {
        long finalAmount = discountedTotalPrice + totalDeliveryFee;
        // 100% 할인 등으로 최종 결제 금액이 0원이 될 경우, 최소 결제 금액 1원으로 보정
        return Math.max(finalAmount, 1L);
    }

    private Orders createAndSaveOrderOnly(Buyers buyer, Long subTotalPrice, Long discountAmount, Long finalPaymentAmount, Long totalDeliveryFee) {
        Orders order = Orders.builder()
                .buyers(buyer)
                .orderNumber(generateOrderNumber()) // String 타입 반환값 사용
                .orderStatus(OrderStatus.PAYMENT_PENDING)
                .subtotalPrice(subTotalPrice)
                .totalDeliveryFee(totalDeliveryFee)
                .totalDiscountAmount(discountAmount)
                .discountedTotalPrice(finalPaymentAmount)
                .build();
        return orderRepository.save(order);
    }

    private void saveOrderPendingDetails(Orders order, Buyers buyer,
                                         Long originalTotalPrice,
                                         Long totalDeliveryFee,
                                         Long finalPaymentAmount,
                                         OrderCreateRequest.PaymentInfoRequest paymentInfo,
                                         Map<Sellers, List<OrderItemInfo>> orderItems,
                                         OrderCreateRequest.ShippingAddressRequest shippingAddress) {
        try {
            List<OrderItemSnapshot> snap = orderItems.values()
                    .stream()
                    .flatMap(List::stream)
                    .map(i -> new OrderItemSnapshot(
                            i.productId(),
                            i.productName(),
                            i.quantity(),
                            i.unitPrice(),
                            i.totalPrice(),
                            i.sellerId()        // ← String
                    ))
                    .toList();
            String orderItemsJson = objectMapper.writeValueAsString(snap);
            String shippingAddressJson = objectMapper.writeValueAsString(shippingAddress);
            String appliedCouponsJson = getAppliedCouponsJson(paymentInfo);
            OrderPendingDetails orderPendingDetails = OrderPendingDetails.builder()
                    .orders(order)
                    .buyers(buyer)
                    .originalTotalPrice(originalTotalPrice)
                    .totalDeliveryFee(totalDeliveryFee)
                    .finalPaymentAmount(finalPaymentAmount)
                    .orderItemsJson(orderItemsJson)
                    .appliedCouponsJson(appliedCouponsJson)
                    .shippingAddressJson(shippingAddressJson).build();
            orderPendingDetailsRepository.save(orderPendingDetails);
            log.debug("OrderPendingDetails 저장 완료: orderId={}, 쿠폰정보={}",
                    order.getId(), appliedCouponsJson);

        } catch (JsonProcessingException e) {
            log.error("OrderPendingDetails JSON 직렬화 실패: orderId={}, error={}", order.getId(), e.getMessage());
            throw new RuntimeException("주문 정보 저장 중 오류가 발생했습니다", e);
        }
    }

    private String getAppliedCouponsJson(OrderCreateRequest.PaymentInfoRequest paymentInfo) throws JsonProcessingException {
        if (paymentInfo != null && paymentInfo.getSellerCoupons() != null) {
            return objectMapper.writeValueAsString(paymentInfo.getSellerCoupons());
        }
        return objectMapper.writeValueAsString(List.of());
    }


    @Override
    @JpaTransactional
    public OrderDeleteResponse deleteOrder(UserPrincipal userPrincipal, String orderNumber) {
        log.info("주문 내역 삭제 요청 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        try {
            Buyers buyer = findUserByPrincipal(userPrincipal);
            Orders order = orderRepository.findOrderDetailByUserAndOrderNumber(buyer, orderNumber)
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


    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomSuffix = ThreadLocalRandom.current().nextInt(10000, 100000);
        return timestamp + randomSuffix;
    }
}