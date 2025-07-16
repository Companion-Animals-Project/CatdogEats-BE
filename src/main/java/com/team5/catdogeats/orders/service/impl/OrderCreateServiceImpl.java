package com.team5.catdogeats.orders.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.repository.BuyerCouponRepository;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderPendingDetails;
import com.team5.catdogeats.orders.dto.GroupSellerAndCouponsDTO;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.dto.common.OrderItemSnapshot;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderPendingDetailsRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.OrderCreateService;
import com.team5.catdogeats.orders.util.OrderResponseBuilder;
import com.team5.catdogeats.orders.util.OrderCreateUtils;
import com.team5.catdogeats.outbox.domain.OutboxMessage;
import com.team5.catdogeats.outbox.repository.OutboxMessageRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.pets.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCreateServiceImpl implements OrderCreateService {

    private final OrderRepository orderRepository;
    private final OrderPendingDetailsRepository orderPendingDetailsRepository;
    private final BuyerRepository buyerRepository;
    private final ProductRepository productRepository;
    private final SellersRepository sellersRepository;
    private final OrderResponseBuilder orderResponseBuilder;
    private final ObjectMapper objectMapper;
    private final BuyerCouponRepository buyerCouponRepository;
    private final OutboxMessageRepository outboxMessageRepository;
    private final PetRepository petsRepository;

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
            Long originalTotalPrice = OrderCreateUtils.calculateOriginalTotalPrice(validatedOrderItems);
            Long discountAmount = OrderCreateUtils.applyCouponDiscount(coupons, validatedOrderItems);

            Long totalDeliveryFee = OrderCreateUtils.calculateTotalDeliveryFee(validatedOrderItems);
            Long discountedTotalPrice = originalTotalPrice - discountAmount;
            Long finalPaymentAmount = discountedTotalPrice + totalDeliveryFee;

            log.debug("주문 금액 계산: 원가={}원, 할인후={}원, 최종={}원",
                    originalTotalPrice, discountedTotalPrice, finalPaymentAmount);
            // 반려동물 정보 조회 및 검증
            Pets pet = petsRepository.findById(request.getPetId())
                    .orElseThrow(() -> new IllegalArgumentException("반려동물을 찾을 수 없습니다: " + request.getPetId()));

// 구매자 본인의 반려동물인지 확인
            if (!pet.getBuyer().getUser().getId().equals(buyers.getUser().getId())) {
                throw new IllegalArgumentException("본인의 반려동물만 선택할 수 있습니다");
            }
            // 5. Orders 엔티티만 생성 및 저장 (PAYMENT_PENDING 상태)
            Orders savedOrder = createAndSaveOrderOnly(buyers, pet, originalTotalPrice, discountAmount, finalPaymentAmount, totalDeliveryFee);

            // 6. OrderPendingDetails에 임시 정보 저장 (메서드 시그니처 변경)
            saveOrderPendingDetails(savedOrder, buyers, originalTotalPrice, totalDeliveryFee, finalPaymentAmount,
                    request.getPaymentInfo(), validatedOrderItems, request.getShippingAddress());

            log.debug("주문 생성 완료: orderId={}, orderNumber={}, status={}, 최종금액={}원",
                    savedOrder.getId(), savedOrder.getOrderNumber(), savedOrder.getOrderStatus(), finalPaymentAmount);


            List<OrderItemInfo> flatItems = validatedOrderItems.values().stream()
                    .flatMap(List::stream)
                    .toList();

            // 6. OrderCreatedEvent 발행 (재고 예약 및 결제 정보 생성용)
            OrderCreatedEvent event = getOrderCreatedEvent(userPrincipal, savedOrder, buyers, finalPaymentAmount, flatItems);
            OutboxMessage createMessage = orderResponseBuilder.orderCteateOutboxMessage(savedOrder, event);
            outboxMessageRepository.save(createMessage);
            OutboxMessage timeOutMessage = orderResponseBuilder.timeOutOutboxMessage(savedOrder, event);
            outboxMessageRepository.save(timeOutMessage);


            return getOrderCreateResponse(request, validatedOrderItems, savedOrder);

        } catch (NoSuchElementException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("주문 생성 실패: error={}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private OrderCreatedEvent getOrderCreatedEvent(UserPrincipal userPrincipal,
                                                   Orders savedOrder,
                                                   Buyers buyers,
                                                   Long finalPaymentAmount,
                                                   List<OrderItemInfo> flatItems) {
        return OrderCreatedEvent.of(
                savedOrder.getId(),
                savedOrder.getOrderNumber(),
                buyers.getUserId(),
                userPrincipal.provider(),
                userPrincipal.providerId(),
                finalPaymentAmount,
                flatItems
        );
    }


    private OrderCreateResponse getOrderCreateResponse(OrderCreateRequest request, Map<Sellers, List<OrderItemInfo>> validatedOrderItems, Orders savedOrder) {
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


        return orderResponseBuilder.buildTossPaymentResponse(
                savedOrder,
                request.getPaymentInfo(),
                orderName
        );
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



    private Buyers findUserByPrincipal(UserPrincipal userPrincipal) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("구매자를 찾을 수 없거나 권한이 없습니다"));
        return BuyerDTO.toEntity(buyerDTO);
    }

    private Map<Sellers, List<OrderItemInfo>> validateAndCollectOrderItems(
            List<OrderCreateRequest.OrderItemRequest> reqs){

        // 1. 상품 ID 목록 추출
        Set<String> productIds = reqs.stream()
                .map(OrderCreateRequest.OrderItemRequest::getProductId)
                .collect(Collectors.toSet());

        // 2. 모든 상품 한 번에 조회
        Map<String, Products> productsMap = productRepository.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Products::getId, p -> p));

        // 3. 판매자 ID 수집
        Set<String> sellerIds = productsMap.values().stream()
                .map(p -> p.getSeller().getUserId())
                .collect(Collectors.toSet());

        // 4. 모든 판매자 한 번에 조회
        Map<String, Sellers> sellersMap = sellersRepository.findAllById(sellerIds)
                .stream()
                .collect(Collectors.toMap(Sellers::getUserId, s -> s));

        // 5. 주문 상품 생성 및 검증
        List<OrderItemInfo> rawItems = reqs.stream()
                .map(r -> {
                    if (r.getQuantity() <= 0) {
                        throw new IllegalArgumentException("수량은 1개 이상");
                    }

                    Products p = productsMap.get(r.getProductId());
                    if (p == null) {
                        throw new NoSuchElementException("상품 없음: " + r.getProductId());
                    }

                    // OrderItemInfo에는 sellerId만 포함
                    return OrderItemInfo.of(p, r.getQuantity());
                })
                .peek(i -> {
                    Sellers seller = sellersMap.get(i.sellerId());
                    if (seller.isDeleted()) {
                        throw new IllegalStateException("탈퇴한 판매자의 상품은 구매할 수 없습니다.");
                    }
                })
                .toList();

        // 6. 같은 상품 병합
        Map<String, OrderItemInfo> mergedByProduct = rawItems.stream()
                .collect(Collectors.toMap(
                        OrderItemInfo::productId,
                        i -> i,
                        OrderItemInfo::mergeQuantity
                ));

        // 7. 판매자별로 그룹핑 (sellerId로 Sellers 객체 조회)
        return mergedByProduct.values().stream()
                .collect(Collectors.groupingBy(
                        item -> sellersMap.get(item.sellerId())
                ));

    }

    private Orders createAndSaveOrderOnly(Buyers buyer, Pets pet, Long subTotalPrice, Long discountAmount, Long finalPaymentAmount, Long totalDeliveryFee) {
        Orders order = Orders.builder()
                .buyers(buyer)
                .pet(pet)
                .orderNumber(OrderCreateUtils.generateOrderNumber()) // String 타입 반환값 사용
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
}