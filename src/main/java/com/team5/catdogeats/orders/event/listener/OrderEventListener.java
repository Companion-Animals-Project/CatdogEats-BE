package com.team5.catdogeats.orders.event.listener;

import com.team5.catdogeats.coupons.repository.BuyerCouponRepository;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.outbox.util.IdempotentConsumer;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.event.OrderCreatedEvent;
import com.team5.catdogeats.orders.repository.OrderItemRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.event.PaymentCompletedEvent;
import com.team5.catdogeats.payments.event.PaymentFailedEvent;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.service.ProductStockManager;
import com.team5.catdogeats.products.service.StockReservationService;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final StockReservationService stockReservationService;
    private final ProductStockManager productStockManager;
    private final BuyerCouponRepository buyerCouponRepository;
    private final IdempotentConsumer idempotentConsumer;

    @RabbitListener(
            queues = "#{@orderCreatedQueue.name}",
            containerFactory = "listenerContainerFactory"
    )
    @JpaTransactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStockReservation(OrderCreatedEvent event) {
        String orderId = event.orderId();
        log.info("재고 예약 처리 시작: orderId={}, orderNumber={}, itemCount={}",
                orderId, event.orderNumber(), event.getOrderItemCount());

        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            if (order.getOrderStatus() == OrderStatus.CANCELLED) {
                log.info("취소된 주문 - 재고 예약 건너뜀: orderId={}", orderId);
                return;
            }

            List<StockReservationService.ReservationRequest> reservationRequests =
                    createReservationRequests(event.orderItems());

            stockReservationService.createBulkReservations(order, reservationRequests);

            log.info("재고 예약 완료: orderId={}, itemCount={}", orderId, reservationRequests.size());

        } catch (Exception e) {
            log.error("재고 예약 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            performStockReservationCompensation(orderId, "재고 예약 실패: " + e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @JpaTransactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderItemsAndShipmentsCreation(PaymentCompletedEvent event) {
        String orderId = event.orderId();
        log.info("주문 완료 처리 시작: orderId={}, orderNumber={}, itemCount={}",
                orderId, event.orderNumber(), event.getOrderItemCount());

        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            List<OrderItems> orderItems = createOrderItems(order, event.orderItems());
            orderItemRepository.saveAll(orderItems);
            log.debug("OrderItems 생성 완료: orderId={}, itemCount={}", orderId, orderItems.size());

            if (event.shippingAddress() != null) {
                Sellers seller = getSellerFromOrderItems(orderItems);
                Shipments shipment = createShipment(order, event.shippingAddress(), seller);
                shipmentRepository.save(shipment);
                log.debug("Shipments 생성 완료: orderId={}, shipmentId={}, 수령인={}",
                        orderId, shipment.getId(), shipment.getRecipientName());
            }

            if (event.couponApplied() && !event.couponIds().isEmpty()) {
                int updated = buyerCouponRepository.markBuyerCouponUsed(order.getBuyers(), event.couponIds(), ZonedDateTime.now());
                if (updated != event.couponIds().size()) {
                    throw new IllegalStateException("쿠폰 소모 처리 불일치");
                }
            }


            order.setOrderStatus(OrderStatus.PAYMENT_COMPLETED);
            orderRepository.save(order);
            log.info("주문 상태 변경 완료: orderId={}, status={}", orderId, OrderStatus.PAYMENT_COMPLETED);

            stockReservationService.confirmReservations(orderId);
            productStockManager.decrementStockForConfirmedReservations(orderId);
            log.info("재고 확정 완료: orderId={}", orderId);
            log.info("결제 완료 처리 전체 완료: orderId={} ✅", orderId);

        } catch (Exception e) {
            log.error("결제 완료 처리 실패: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    @Async
    @EventListener
    public void handlePaymentCompletedNotification(PaymentCompletedEvent event) {
        String orderId = event.orderId();
        log.info("결제 완료 알림 처리 시작: orderId={}, orderNumber={}",
                orderId, event.orderNumber());

        try {
            String productInfo = event.getFirstProductName() +
                    (event.getOrderItemCount() > 1 ?
                            String.format(" 외 %d개", event.getOrderItemCount() - 1) : "");

            // 할인 정보 개선 (쿠폰 타입 지원)


            log.info("""
                    [Catdogeats] 결제가 완료되었습니다! 🎉
                    주문번호: {}
                    상품: {}{}
                    결제 금액: {}원
                    상품 준비를 시작합니다.
                    """,
                    event.orderNumber(),
                    productInfo,
                    event.discountAmount(),
                    String.format("%,d", event.discountedTotalPrice())
            );


            log.info("결제 완료 알림 발송 완료: orderId={}, paymentId={}, itemCount={}",
                    orderId, event.paymentId(), event.getOrderItemCount());

        } catch (Exception e) {
            log.error("결제 완료 알림 발송 실패: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    @RabbitListener(
            queues = "#{@paymentFailedQueue.name}",
            containerFactory = "listenerContainerFactory"
    )
    @JpaTransactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailure(PaymentFailedEvent event, Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        String orderId = event.orderId();

        if (!idempotentConsumer.processOnce(messageId, "payment-failure-listener")) {
            log.info("이미 처리된 결제 실패 메시지입니다: messageId={}, orderId={}", messageId, orderId);
            return;
        }

        log.info("결제 실패 처리 시작: orderId={}, orderNumber={}, error={}",
                orderId, event.orderNumber(), event.errorMessage());

        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            paymentRepository.findByOrdersId(orderId)
                    .ifPresent(payment -> {
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                        log.info("결제 정보 실패 상태 업데이트: paymentId={}", payment.getId());
                    });

            stockReservationService.cancelReservations(orderId);
            log.info("결제 실패 처리 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    private List<StockReservationService.ReservationRequest> createReservationRequests(List<OrderItemInfo> orderItems) {
        return orderItems.stream()
                .map(orderItem -> {
                    Products product = productRepository.findById(orderItem.productId())
                            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + orderItem.productId()));
                    return new StockReservationService.ReservationRequest(product, orderItem.quantity());
                })
                .toList();
    }

    private List<OrderItems> createOrderItems(Orders order, List<OrderItemInfo> orderItemInfos) {
        return orderItemInfos.stream()
                .map(orderItemInfo -> {
                    Products product = productRepository.findById(orderItemInfo.productId())
                            .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다: " + orderItemInfo.productId()));

                    return OrderItems.builder()
                            .orders(order)
                            .products(product)
                            .quantity(orderItemInfo.quantity())
                            .price(orderItemInfo.unitPrice())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Shipments createShipment(Orders order, OrderCreateRequest.ShippingAddressRequest shippingAddress, Sellers seller) {
        return Shipments.builder()
                .orders(order)
                .buyers(order.getBuyers())
                .seller(seller)
                .recipientName(shippingAddress.getRecipientName())
                .recipientPhone(shippingAddress.getRecipientPhone())
                .postalCode(shippingAddress.getPostalCode())
                .shippingAddress(shippingAddress.getStreetAddress())
                .detailAddress(shippingAddress.getDetailAddress())
                .deliveryNote(shippingAddress.getDeliveryNote())
                .build();
    }

    @JpaTransactional(propagation = Propagation.REQUIRES_NEW)
    public void performStockReservationCompensation(String orderId, String reason) {
        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("보상 처리할 주문을 찾을 수 없습니다: " + orderId));

            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            log.warn("재고 예약 실패 보상 처리 완료: orderId={}, reason={}", orderId, reason);

        } catch (Exception e) {
            log.error("재고 예약 보상 처리 실패: orderId={}, reason={}, error={}",
                    orderId, reason, e.getMessage(), e);
        }
    }
    /**
     * OrderItems에서 seller 정보 추출
     */
    private Sellers getSellerFromOrderItems(List<OrderItems> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            log.warn("OrderItems가 비어있어 seller 정보를 가져올 수 없습니다");
            return null;
        }

        OrderItems firstOrderItem = orderItems.get(0);
        return firstOrderItem.getProducts().getSeller();
    }
}