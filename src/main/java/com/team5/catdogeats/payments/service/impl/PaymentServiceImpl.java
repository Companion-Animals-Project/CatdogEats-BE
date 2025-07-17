package com.team5.catdogeats.payments.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.mapping.OrderPendingDetails;
import com.team5.catdogeats.orders.dto.common.GroupOrdersAndPayments;
import com.team5.catdogeats.orders.dto.common.OrderItemInfo;
import com.team5.catdogeats.orders.dto.common.OrderItemSnapshot;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.repository.OrderPendingDetailsRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.outbox.domain.OutboxMessage;
import com.team5.catdogeats.outbox.domain.enums.OutboxStatus;
import com.team5.catdogeats.outbox.repository.OutboxMessageRepository;
import com.team5.catdogeats.payments.client.TossPaymentsClient;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.dto.request.TossPaymentConfirmRequest;
import com.team5.catdogeats.payments.dto.response.PaymentConfirmResponse;
import com.team5.catdogeats.payments.dto.response.TossPaymentConfirmResponse;
import com.team5.catdogeats.payments.event.CouponInfo;
import com.team5.catdogeats.payments.event.PaymentCompletedEvent;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.payments.service.PaymentFailService;
import com.team5.catdogeats.payments.service.PaymentService;
import com.team5.catdogeats.payments.util.PaymentUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

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
    private final ObjectMapper objectMapper;
    private final OutboxMessageRepository outboxMessageRepository;
    private final PaymentFailService paymentFailService;

    @Override
    @JpaTransactional
    public PaymentConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount) {
        log.debug("결제 승인 처리 시작: paymentKey={}, orderId={}, amount={}",
                paymentKey, orderId, amount);

        try {
            // 1. 주문 및 결제 정보 조회
            GroupOrdersAndPayments groupOrdersAndPayments = orderRepository.findGroupByOrdersAndPaymentsOrderId(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문과 일치하는 결제 정보가 없습니다."));

            // 2. 결제 상태 및 금액 검증
            PaymentUtils.validatePaymentStatus(groupOrdersAndPayments.payments(), groupOrdersAndPayments.orders() );
            PaymentUtils.validatePaymentAmount(groupOrdersAndPayments.orders(), amount);

            // 3. Toss Payments API 호출
            TossPaymentConfirmResponse tossResponse = callTossPaymentConfirm(paymentKey, orderId, amount);
            PaymentUtils.validateTossResponse(tossResponse, groupOrdersAndPayments.orders(), amount);

            // 4. 결제 정보 업데이트 (PaymentStatus.SUCCESS로 변경)
            updatePaymentStatus(groupOrdersAndPayments.payments(), tossResponse);

            log.info("결제 승인 완료: orderId={}, paymentId={}, tossPaymentKey={}",
                    orderId, groupOrdersAndPayments.payments().getId(), tossResponse.getPaymentKey());

            createPaymentCompletedOutbox(groupOrdersAndPayments.orders(), groupOrdersAndPayments.payments(), tossResponse);
            log.info("PaymentCompletedEvent Outbox 메시지 생성 완료: orderId={}", orderId);

            // 6. OrderPendingDetails 정리 (결제 완료 후 더 이상 필요 없음)
            orderPendingDetailsRepository.deleteByOrderId(orderId);
            log.debug("OrderPendingDetails 정리 완료: orderId={}", orderId);

            // 7. 응답 생성
            return PaymentConfirmResponse.builder()
                    .paymentId(groupOrdersAndPayments.payments().getId())
                    .orderId(orderId)
                    .orderNumber(groupOrdersAndPayments.orders().getOrderNumber())
                    .amount(amount)
                    .status(PaymentStatus.SUCCESS)
                    .paidAt(ZonedDateTime.now())
                    .tossPaymentKey(tossResponse.getPaymentKey())
                    .message("결제가 성공적으로 완료되었습니다.")
                    .build();

        } catch (Exception e) {
            try {
                paymentFailService.handlePaymentFailure(orderId, "PAYMENT_ERROR", e.getMessage());

                // 실패 응답 생성
                return PaymentConfirmResponse.builder()
                        .orderId(orderId)
                        .status(PaymentStatus.FAILED)
                        .message("결제 승인에 실패했습니다: " + e.getMessage())
                        .build();

            } catch (Exception ex) {
                log.error("결제 실패 처리 중 추가 오류 발생: orderId={}, error={}", orderId, ex.getMessage(), ex);

                return PaymentConfirmResponse.builder()
                        .orderId(orderId)
                        .status(PaymentStatus.FAILED)
                        .message("결제 승인에 실패했습니다: " + e.getMessage())
                        .build();
            }
        }
    }


    // === createPaymentCompletedOutbox 발행 ===
    private void createPaymentCompletedOutbox(Orders order, Payments payment, TossPaymentConfirmResponse tossResponse) {
        try {
            OrderPendingDetails pendingDetails = orderPendingDetailsRepository.findByOrderId(order.getId())
                    .orElseThrow(() -> new NoSuchElementException("주문 대기 정보를 찾을 수 없습니다: " + order.getId()));

            List<OrderItemSnapshot> snap = objectMapper.readValue(
                    pendingDetails.getOrderItemsJson(),
                    new TypeReference<>() {}
            );

            List<String> couponIds = List.of();      // 기본값
            if (pendingDetails.getAppliedCouponsJson() != null && !pendingDetails.getAppliedCouponsJson().isBlank()) {
                couponIds = objectMapper.readValue(
                        pendingDetails.getAppliedCouponsJson(),
                        new TypeReference<>() {
                        }
                );
            }

            List<OrderItemInfo> orderItems = snap.stream()
                    .map(s -> OrderItemInfo.of(
                            s.productId(), s.productName(), s.quantity(),
                            s.unitPrice(), s.totalPrice(),
                            s.sellerId()
                    )).toList();

            OrderCreateRequest.ShippingAddressRequest shippingAddress = Optional
                    .ofNullable(pendingDetails.getShippingAddressJson())
                    .filter(str -> !str.isBlank())
                    .map(json -> {
                        try {
                            return objectMapper.readValue(json, OrderCreateRequest.ShippingAddressRequest.class);
                        } catch (JsonProcessingException ex) {
                            throw new RuntimeException(ex);
                        }
                    })
                    .orElse(null);

            CouponInfo couponInfo = parseCouponInfo(pendingDetails.getAppliedCouponsJson(), order);

            PaymentCompletedEvent event = PaymentCompletedEvent.of(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getBuyers().getUserId(),
                    payment.getId(),
                    tossResponse.getPaymentKey(),
                    orderItems,
                    shippingAddress,
                    pendingDetails.getOriginalTotalPrice(),
                    couponInfo.applied(),
                    couponInfo.discountAmount(),
                    order.getDiscountedTotalPrice(),
                    couponIds
            );

            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .aggregateId(payment.getId())
                    .aggregateType("PAYMENT")
                    .eventType("payment.completed")
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxMessageRepository.save(outboxMessage);
            log.debug("결제 완료 Outbox 메시지 생성 완료: paymentId={}, outboxId={}",
                    payment.getId(), outboxMessage.getId());



        } catch (JsonProcessingException e) {
            log.error("PaymentCompletedEvent 발행 실패 (JSON 역직렬화 오류): orderId={}, error={}",
                    order.getId(), e.getMessage());
            throw new RuntimeException("결제 완료 이벤트 발행 중 오류가 발생했습니다", e);

        } catch (Exception e) {
            log.error("PaymentCompletedEvent 발행 실패: orderId={}, error={}", order.getId(), e.getMessage());
            throw new RuntimeException("결제 완료 이벤트 발행 중 오류가 발생했습니다", e);
        }
    }

    private CouponInfo parseCouponInfo(String appliedCouponsJson, Orders order) {
        // 기본값 설정
        if (appliedCouponsJson == null || appliedCouponsJson.trim().isEmpty()) {
            return new CouponInfo(false, 0L);
        }

        try {
            // 1. List<String> 형태로 파싱 시도 (OrderCreateRequest.PaymentInfoRequest.getSellerCoupons()가 List<String>인 경우)
            List<String> sellerCoupons = objectMapper.readValue(
                    appliedCouponsJson,
                    new TypeReference<List<String>>() {}
            );

            boolean applied = !sellerCoupons.isEmpty();
            Long discountAmount = applied ? order.getTotalDiscountAmount() : 0L;

            log.debug("쿠폰 정보 파싱 성공 (List<String>): 쿠폰적용={}, 할인금액={}원", applied, discountAmount);
            return new CouponInfo(applied, discountAmount);

        } catch (JsonProcessingException e2) {
                log.warn("쿠폰 정보 파싱 실패, 기본값 사용: appliedCouponsJson={}, error={}",
                        appliedCouponsJson, e2.getMessage());
                return new CouponInfo(false, 0L);
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

    private void updatePaymentStatus(Payments payment, TossPaymentConfirmResponse response) {
        payment.updatePaymentStatus(response);
        paymentRepository.save(payment);

        log.debug("결제 정보 업데이트 완료: paymentId={}, status={}, tossPaymentKey={}",
                payment.getId(), payment.getStatus(), response.getPaymentKey());
    }
}