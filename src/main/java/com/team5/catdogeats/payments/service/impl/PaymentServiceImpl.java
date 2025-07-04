package com.team5.catdogeats.payments.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.OrderPendingDetails;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.repository.OrderPendingDetailsRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.payments.client.TossPaymentsClient;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.dto.request.TossPaymentConfirmRequest;
import com.team5.catdogeats.payments.dto.response.PaymentConfirmResponse;
import com.team5.catdogeats.payments.dto.response.TossPaymentConfirmResponse;
import com.team5.catdogeats.payments.event.PaymentCompletedEvent;
import com.team5.catdogeats.payments.event.PaymentFailedEvent;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.payments.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 결제 처리 서비스 구현체 (완전한 리팩토링 버전)
 * 주요 변경사항:
 * - 결제 승인 시 PaymentCompletedEvent 발행
 * - OrderPendingDetails에서 orderItems와 shippingAddress 정보 조회
 * - 실제 OrderItems, Shipments 생성은 이벤트 리스너에서 처리
 * - 주문 상태 변경도 이벤트 리스너에서 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderPendingDetailsRepository orderPendingDetailsRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @JpaTransactional
    public PaymentConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount) {
        log.info("결제 승인 처리 시작: paymentKey={}, orderId={}, amount={}",
                paymentKey, orderId, amount);

        try {
            // 1. 주문 및 결제 정보 조회
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));
            Payments payment = paymentRepository.findByOrdersId(orderId)
                    .orElseThrow(() -> new NoSuchElementException("결제 정보를 찾을 수 없습니다: " + orderId));

            // 2. 결제 상태 및 금액 검증
            validatePaymentStatus(payment, order);
            validatePaymentAmount(order, amount);

            // 3. Toss Payments API 호출
            TossPaymentConfirmResponse tossResponse = callTossPaymentConfirm(paymentKey, orderId, amount);
            validateTossResponse(tossResponse, order, amount);

            // 4. 결제 정보 업데이트 (PaymentStatus.SUCCESS로 변경)
            updatePaymentStatus(payment, tossResponse);

            log.info("결제 승인 완료: orderId={}, paymentId={}, tossPaymentKey={}",
                    orderId, payment.getId(), tossResponse.getPaymentKey());

            // 5. PaymentCompletedEvent 발행
            publishPaymentCompletedEvent(order, payment, tossResponse);

            // 6. OrderPendingDetails 정리 (결제 완료 후 더 이상 필요 없음)
            orderPendingDetailsRepository.deleteByOrderId(orderId);
            log.debug("OrderPendingDetails 정리 완료: orderId={}", orderId);

            // 7. 응답 생성
            return PaymentConfirmResponse.builder()
                    .paymentId(payment.getId())
                    .orderId(orderId)
                    .orderNumber(order.getOrderNumber())
                    .amount(amount)
                    .status(PaymentStatus.SUCCESS)
                    .paidAt(ZonedDateTime.now())
                    .tossPaymentKey(tossResponse.getPaymentKey())
                    .message("결제가 성공적으로 완료되었습니다.")
                    .build();

        } catch (Exception e) {
            log.error("결제 승인 실패: orderId={}, error={}", orderId, e.getMessage(), e);

            // 결제 실패 이벤트 발행
            publishPaymentFailedEvent(orderId, e.getMessage());

            throw new RuntimeException("결제 승인에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    @JpaTransactional
    public void handlePaymentFailure(String orderId, String code, String message) {
        log.info("결제 실패 처리 시작: orderId={}, code={}, message={}", orderId, code, message);

        try {
            // PaymentFailedEvent 발행 (실제 처리는 이벤트 리스너에서)
            publishPaymentFailedEvent(orderId, code + ": " + message);

            // OrderPendingDetails 정리
            orderPendingDetailsRepository.deleteByOrderId(orderId);
            log.debug("OrderPendingDetails 정리 완료: orderId={}", orderId);

            log.info("결제 실패 처리 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    // === PaymentCompletedEvent 발행 ===
    private void publishPaymentCompletedEvent(Orders order, Payments payment, TossPaymentConfirmResponse tossResponse) {
        try {
            // OrderPendingDetails에서 임시 저장된 정보 조회
            OrderPendingDetails pendingDetails = orderPendingDetailsRepository.findByOrderId(order.getId())
                    .orElseThrow(() -> new NoSuchElementException("주문 대기 정보를 찾을 수 없습니다: " + order.getId()));

            // JSON 역직렬화
            List<OrderItemInfo> orderItems = objectMapper.readValue(
                    pendingDetails.getOrderItemsJson(),
                    new TypeReference<List<OrderItemInfo>>() {}
            );

            OrderCreateRequest.ShippingAddressRequest shippingAddress = null;
            if (pendingDetails.getShippingAddressJson() != null) {
                shippingAddress = objectMapper.readValue(
                        pendingDetails.getShippingAddressJson(),
                        OrderCreateRequest.ShippingAddressRequest.class
                );
            }

            // PaymentCompletedEvent 발행
            PaymentCompletedEvent event = PaymentCompletedEvent.of(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getUser().getId(),
                    pendingDetails.getUserProvider(),
                    pendingDetails.getUserProviderId(),
                    payment.getId(),
                    tossResponse.getPaymentKey(),
                    order.getTotalPrice(),
                    orderItems,
                    shippingAddress,
                    pendingDetails.getOriginalTotalPrice(),
                    pendingDetails.getCouponDiscountRate()
            );

            eventPublisher.publishEvent(event);

            log.info("PaymentCompletedEvent 발행 완료: orderId={}, paymentId={}, itemCount={}, 배송지={}",
                    order.getId(), payment.getId(), orderItems.size(),
                    shippingAddress != null ? shippingAddress.getRecipientName() : "없음");

        } catch (JsonProcessingException e) {
            log.error("PaymentCompletedEvent 발행 실패 (JSON 역직렬화 오류): orderId={}, error={}",
                    order.getId(), e.getMessage());
            throw new RuntimeException("결제 완료 이벤트 발행 중 오류가 발생했습니다", e);

        } catch (Exception e) {
            log.error("PaymentCompletedEvent 발행 실패: orderId={}, error={}", order.getId(), e.getMessage());
            throw new RuntimeException("결제 완료 이벤트 발행 중 오류가 발생했습니다", e);
        }
    }

    // === PaymentFailedEvent 발행 ===
    private void publishPaymentFailedEvent(String orderId, String errorMessage) {
        try {
            Orders order = orderRepository.findById(orderId)
                    .orElse(null);

            Long orderNumber = order != null ? order.getOrderNumber() : null;

            PaymentFailedEvent event = PaymentFailedEvent.of(
                    orderId,
                    orderNumber,
                    "PAYMENT_FAILED",
                    errorMessage
            );

            eventPublisher.publishEvent(event);
            log.debug("PaymentFailedEvent 발행 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("PaymentFailedEvent 발행 실패: orderId={}, error={}", orderId, e.getMessage());
        }
    }

    // === 검증 메서드들 ===

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
                    String.format("결제 금액이 일치하지 않습니다. 주문금액: %d, 결제요청금액: %d",
                            order.getTotalPrice(), amount)
            );
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

    private void validateTossResponse(TossPaymentConfirmResponse response, Orders order, Long amount) {
        if (response == null) {
            throw new RuntimeException("Toss Payments API 응답이 null입니다");
        }

        if (!response.getOrderId().equals(order.getId())) {
            throw new RuntimeException("응답의 주문 ID가 일치하지 않습니다");
        }

        if (!response.getTotalAmount().equals(amount)) {
            throw new RuntimeException("응답의 결제 금액이 일치하지 않습니다");
        }
    }

    private void updatePaymentStatus(Payments payment, TossPaymentConfirmResponse response) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTossPaymentKey(response.getPaymentKey());
        payment.setPaidAt(ZonedDateTime.now());
        paymentRepository.save(payment);

        log.debug("결제 정보 업데이트 완료: paymentId={}, status={}, tossPaymentKey={}",
                payment.getId(), payment.getStatus(), response.getPaymentKey());
    }
}