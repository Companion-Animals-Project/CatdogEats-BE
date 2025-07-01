package com.team5.catdogeats.payments.service.impl;

import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.repository.OrderItemRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.payments.client.TossPaymentsClient;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.dto.request.TossPaymentConfirmRequest;
import com.team5.catdogeats.payments.dto.response.PaymentConfirmResponse;
import com.team5.catdogeats.payments.dto.response.TossPaymentConfirmResponse;
import com.team5.catdogeats.payments.event.PaymentCompletedEvent;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.payments.service.PaymentService;
import com.team5.catdogeats.products.service.ProductStockManager;
import com.team5.catdogeats.products.service.StockReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

// 결제 처리 서비스 구현체 (타입 수정됨)
// Orders 엔티티의 ID 타입이 String으로 변경됨에 따라 관련 메서드들을 수정하였습니다.
// Toss Payments API와 연동하여 결제 승인, 검증 및 상태 관리를 담당합니다.
// 프로젝트 컨벤션에 따라 인터페이스 + 구현체 패턴을 적용했습니다.
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final StockReservationService stockReservationService;
    private final ProductStockManager productStockManager;
    private final TossPaymentsClient tossPaymentsClient;

    // 장바구니에서 구매제품 삭제
    private final ApplicationEventPublisher eventPublisher;
    private final OrderItemRepository orderItemRepository;

    @Override
    @JpaTransactional
    public PaymentConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount) {
        log.info("결제 승인 처리 시작: paymentKey={}, orderId={}, amount={}",
                paymentKey, orderId, amount);

        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));
        Payments payment = paymentRepository.findByOrdersId(orderId)
                .orElseThrow(() -> new NoSuchElementException("결제 정보를 찾을 수 없습니다: " + orderId));

        validatePaymentStatus(payment, order);
        validatePaymentAmount(order, amount);

        TossPaymentConfirmResponse tossResponse = callTossPaymentConfirm(paymentKey, orderId, amount);
        validateTossResponse(tossResponse, order, amount);

        updateOrderStatus(order, OrderStatus.PAYMENT_COMPLETED);
        updatePaymentStatus(payment, tossResponse);

        stockReservationService.confirmReservations(orderId);
        productStockManager.decrementStockForConfirmedReservations(orderId); // 수정

        // 결제 완료 이벤트 발행 (구매 상품 정보 포함)
        try {
            // 구매한 상품 ID 목록 조회
            List<String> purchasedProductIds = orderItemRepository.findByOrdersId(orderId)
                    .stream()
                    .map(orderItem -> orderItem.getProducts().getId())
                    .collect(Collectors.toList());

            // 이벤트 생성 및 발행
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .paymentId(payment.getId())
                    .orderId(order.getId())
                    .userId(order.getUser().getId())
                    .purchasedProductIds(purchasedProductIds)
                    .totalAmount(payment.getAmount())
                    .completedAt(payment.getPaidAt())
                    .build();

            eventPublisher.publishEvent(event);
            log.info("결제 완료 이벤트 발행 완료 (구매 상품 {}개): {}",
                    purchasedProductIds.size(), event);

        } catch (Exception e) {
            // 이벤트 발행 실패가 결제에 영향을 주지 않도록 예외 처리
            log.error("결제 완료 이벤트 발행 실패 - paymentId: {}, orderId: {}, error: {}",
                    payment.getId(), order.getId(), e.getMessage(), e);
        }

        log.info("결제 승인 완료: orderId={}, paymentId={}, tossPaymentKey={}",
                orderId, payment.getId(), tossResponse.getPaymentKey());

        return PaymentConfirmResponse.builder()
                .paymentId(payment.getId())
                .orderId(orderId)
                .orderNumber(order.getOrderNumber())
                .amount(amount)
                .status(PaymentStatus.SUCCESS)
                .paidAt(ZonedDateTime.now())
                .tossPaymentKey(tossResponse.getPaymentKey())
                .build();
    }

    @Override
    @JpaTransactional
    public void handlePaymentFailure(String orderId, String code, String message) {
        log.info("결제 실패 처리 시작: orderId={}, code={}, message={}", orderId, code, message);

        try {
            // 주문 정보 조회 (String 타입으로 직접 사용)
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            // 주문 상태를 취소로 변경
            updateOrderStatus(order, OrderStatus.CANCELLED);

            // 결제 정보가 있다면 실패 상태로 업데이트 (String 타입으로 직접 사용)
            paymentRepository.findByOrdersId(orderId)
                    .ifPresent(payment -> {
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);
                        log.info("결제 정보 실패 상태 업데이트: paymentId={}", payment.getId());
                    });

            // 재고 예약 취소 (String 타입으로 직접 사용)
            stockReservationService.cancelReservations(orderId);

            log.info("결제 실패 처리 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    // === 내부 검증 메서드들 ===

    private void validatePaymentStatus(Payments payment, Orders order) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제가 이미 처리되었습니다: " + payment.getStatus());
        }

        if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("주문 상태가 결제 대기가 아닙니다: " + order.getOrderStatus());
        }
    }

    private void validatePaymentAmount(Orders order, Long amount) {
        if (!order.getTotalPrice().equals(amount)) {
            throw new IllegalArgumentException(
                    String.format("결제 금액이 일치하지 않습니다: 주문금액=%d, 결제금액=%d",
                            order.getTotalPrice(), amount));
        }
    }

    private TossPaymentConfirmResponse callTossPaymentConfirm(String paymentKey, String orderId, Long amount) {
        TossPaymentConfirmRequest request = TossPaymentConfirmRequest.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount)
                .build();

        return tossPaymentsClient.confirmPayment(request);
    }

    private void validateTossResponse(TossPaymentConfirmResponse tossResponse, Orders order, Long amount) {
        if (!tossResponse.getOrderId().equals(order.getId())) {
            throw new IllegalArgumentException("주문 ID가 일치하지 않습니다");
        }

        if (!tossResponse.getTotalAmount().equals(amount)) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다");
        }
    }

    private void updateOrderStatus(Orders order, OrderStatus status) {
        order.setOrderStatus(status);
        orderRepository.save(order);
        log.info("주문 상태 업데이트: orderId={}, status={}", order.getId(), status);
    }

    private void updatePaymentStatus(Payments payment, TossPaymentConfirmResponse tossResponse) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTossPaymentKey(tossResponse.getPaymentKey());
        payment.setPaidAt(tossResponse.getApprovedAt()); // 결제 승인 시간 설정
        paymentRepository.save(payment);
        log.info("결제 정보 업데이트: paymentId={}, tossPaymentKey={}", payment.getId(), tossResponse.getPaymentKey());
    }
}