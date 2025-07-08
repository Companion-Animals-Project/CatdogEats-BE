package com.team5.catdogeats.payments.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.repository.OrderPendingDetailsRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.outbox.domain.OutboxMessage;
import com.team5.catdogeats.outbox.domain.enums.OutboxStatus;
import com.team5.catdogeats.outbox.repository.OutboxMessageRepository;
import com.team5.catdogeats.payments.event.PaymentFailedEvent;
import com.team5.catdogeats.payments.service.PaymentFailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFailServiceImpl implements PaymentFailService {
    private final OrderRepository orderRepository;
    private final OrderPendingDetailsRepository orderPendingDetailsRepository;
    private final ObjectMapper objectMapper;
    private final OutboxMessageRepository outboxMessageRepository;

    @Override
    @JpaTransactional
    public void handlePaymentFailure(String orderId, String code, String message) {
        log.info("결제 실패 처리 시작: orderId={}, code={}, message={}", orderId, code, message);

        try {
            // PaymentFailedEvent 발행 (실제 처리는 이벤트 리스너에서)
            createPaymentFailedOutbox(orderId, code, message);

            // OrderPendingDetails 정리
            orderPendingDetailsRepository.deleteByOrderId(orderId);
            log.debug("OrderPendingDetails 정리 완료: orderId={}", orderId);

            log.info("결제 실패 처리 완료: orderId={}", orderId);

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    private void createPaymentFailedOutbox(String orderId, String code, String message) {
        try {
            Orders order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));

            PaymentFailedEvent event = PaymentFailedEvent.of(
                    order.getId(),
                    order.getOrderNumber(),
                    code,
                    message
            );

            OutboxMessage outboxMessage = OutboxMessage.builder()
                    .aggregateId(order.getId())
                    .aggregateType("PAYMENT")
                    .eventType("payment.failed")
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxMessageRepository.save(outboxMessage);
            log.debug("결제 실패 Outbox 메시지 생성 완료: orderId={}, outboxId={}",
                    orderId, outboxMessage.getId());

        } catch (JsonProcessingException e) {
            log.error("PaymentFailedEvent 직렬화 실패: orderId={}, error={}", orderId, e.getMessage());
            throw new RuntimeException("결제 실패 이벤트 발행 중 오류가 발생했습니다", e);
        } catch (NoSuchElementException e) {
            throw e;
        } catch (Exception e) {
            log.error("PaymentFailedEvent 발행 실패: orderId={}, error={}", orderId, e.getMessage());
            throw new RuntimeException("결제 실패 이벤트 발행 중 오류가 발생했습니다", e);
        }

    }

}
