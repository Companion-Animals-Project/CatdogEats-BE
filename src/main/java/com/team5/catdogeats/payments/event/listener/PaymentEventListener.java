package com.team5.catdogeats.payments.event.listener;

import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.OrderService;
import com.team5.catdogeats.payments.event.PaymentCompletedEvent;
import com.team5.catdogeats.payments.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.NoSuchElementException;

/**
 * 결제 이벤트 리스너
 * 결제 완료/실패 이벤트를 수신하여 후속 처리를 담당한다
 * - 결제 완료: 주문상세, 배송정보 생성, 상태 업데이트, 성공 알림
 * - 결제 실패: 주문 취소, 실패 알림 (보상 트랜잭션)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderRepository orderRepository;
    // TODO: 5단계에서 구현할 서비스들
    // private final OrderDetailService orderDetailService;
    // private final DeliveryService deliveryService;
    // private final NotificationService notificationService;

    /**
     * 결제 완료 이벤트 처리
     * 주문상세, 배송정보 생성 및 주문 상태를 PAYMENT_COMPLETED로 변경
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        String orderId = event.orderId();
        log.info("결제 완료 이벤트 처리 시작: orderId={}, orderNumber={}, amount={}원",
                orderId, event.orderNumber(), event.amount());

        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            // 주문 상태가 PAYMENT_PENDING인지 확인
            if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
                log.warn("주문 상태가 PAYMENT_PENDING이 아님 - 처리 건너뜀: orderId={}, status={}",
                        orderId, order.getOrderStatus());
                return;
            }

            // TODO: 5단계에서 구현
            // 1. 주문상세 정보 생성
            // orderDetailService.createFrom(orderId);

            // TODO: 5단계에서 구현
            // 2. 배송 정보 준비
            // deliveryService.prepareFor(orderId);

            // 3. 주문 상태를 PAYMENT_COMPLETED로 업데이트
            order.setOrderStatus(OrderStatus.PAYMENT_COMPLETED);
            orderRepository.save(order);

            // TODO: 5단계에서 구현
            // 4. 결제 성공 알림 발송
            // notificationService.sendPaymentSuccess(orderId);

            log.info("결제 완료 처리 성공: orderId={}, 새로운 상태={}",
                    orderId, OrderStatus.PAYMENT_COMPLETED);

        } catch (Exception e) {
            log.error("결제 완료 처리 실패: orderId={}, error={}", orderId, e.getMessage(), e);
            // 이 예외는 payment 트랜잭션에 영향을 주지 않음 (REQUIRES_NEW)
        }
    }

    /**
     * 결제 실패 이벤트 처리
     * 주문을 취소 상태로 변경하고 실패 알림 발송 (보상 트랜잭션)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        String orderId = event.orderId();
        log.info("결제 실패 이벤트 처리 시작: orderId={}, orderNumber={}, failureCode={}",
                orderId, event.orderNumber(), event.failureCode());

        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + orderId));

            // 주문을 취소 상태로 변경 (보상 트랜잭션)
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            // TODO: 5단계에서 구현
            // 결제 실패 알림 발송
            // notificationService.sendPaymentFailure(orderId, event.failureMessage());

            log.info("결제 실패 보상 처리 완료: orderId={}, 새로운 상태={}, 실패사유={}",
                    orderId, OrderStatus.CANCELLED, event.failureMessage());

        } catch (Exception e) {
            log.error("결제 실패 보상 처리 중 오류: orderId={}, error={}", orderId, e.getMessage(), e);
            // 이 예외는 payment 트랜잭션에 영향을 주지 않음 (REQUIRES_NEW)
        }
    }
}