package com.team5.catdogeats.orders.event.listener;

import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.global.config.RabbitMQConfig;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.common.GroupOrdersAndPayments;
import com.team5.catdogeats.orders.event.PaymentTimeoutEvent;
import com.team5.catdogeats.orders.repository.OrderPendingDetailsRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.outbox.util.IdempotentConsumer;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTimeoutListener {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderPendingDetailsRepository pendingDetailsRepository;
    private final StockReservationRepository stockReservationRepository;
    private final IdempotentConsumer idempotentConsumer;



    @RabbitListener(
            queues = RabbitMQConfig.Q_ORDER_PAYMENT_TIMEOUT,
            containerFactory = "listenerContainerFactory"
    )
    @JpaTransactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentTimeout(PaymentTimeoutEvent event, Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        String orderId = event.orderId();
        try {
            if (!idempotentConsumer.processOnce(messageId, "payment-timeout-listener")) {
                log.info("이미 처리된 타임아웃 완료 메시지입니다: messageId={}, orderId={}", messageId, orderId);
                return;
            }

            GroupOrdersAndPayments groupDTO = orderRepository.findGroupByOrdersAndPaymentsOrderId(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문이 없습니다다. " + orderId));

            if (groupDTO.orders().getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
                return;
            }
            stockReservationRepository.deleteByOrderId(groupDTO.orders().getId());
            pendingDetailsRepository.deleteByOrderId(groupDTO.orders().getId());

            groupDTO.payments().updatePaymentStatus(PaymentStatus.TIMEOUT);
            groupDTO.orders().updateOderStatus(OrderStatus.PAYMENT_TIMEOUT);
            paymentRepository.save(groupDTO.payments());
            orderRepository.save(groupDTO.orders());

            log.debug("만료된 주문 삭제 완료 oderId={}", orderId);
        } catch (Exception e) {
            log.error("결제 타임아웃 처리중 예외 발생 {}", e.getMessage());
            throw e;
        }
    }
}
