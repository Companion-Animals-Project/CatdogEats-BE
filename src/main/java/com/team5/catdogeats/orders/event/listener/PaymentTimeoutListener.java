package com.team5.catdogeats.orders.event.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.global.config.RabbitMQConfig;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.common.GroupOrdersAndPayments;
import com.team5.catdogeats.orders.repository.OrderPendingDetailsRepository;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.repository.PaymentRepository;
import com.team5.catdogeats.products.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ObjectMapper objectMapper;



    @RabbitListener(queues = RabbitMQConfig.Q_ORDER_PAYMENT_TIMEOUT)
    @JpaTransactional(propagation = Propagation.REQUIRES_NEW)
    public void onPaymentTimeout(String payload) {
        try {
            String orderId = objectMapper.readTree(payload).get("orderId").asText();


            GroupOrdersAndPayments groupDTO = orderRepository.findGroupByOrdersAndPaymentsOrderId(orderId)
                    .orElseThrow(() -> new NoSuchElementException("주문이 없습니다다. " + orderId));

            if (groupDTO.orders().getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
                return;
            }
            stockReservationRepository.deleteByOrderId(groupDTO.orders().getId());;
            pendingDetailsRepository.deleteByOrderId(groupDTO.orders().getId());;

            groupDTO.payments().updatePaymentStatus(PaymentStatus.TIMEOUT);
            groupDTO.orders().updateOderStatus(OrderStatus.PAYMENT_TIMEOUT);
            paymentRepository.save(groupDTO.payments());
            orderRepository.save(groupDTO.orders());

            log.debug("만료된 주문 삭제 완료 oderId={}", orderId);
        } catch (JsonProcessingException e) {
            log.error("json 파싱 에러 {}", e.getMessage());

        } catch (Exception e) {
            log.error("결제 타임아웃 처리중 예외 발생 {}", e.getMessage());
            throw e;
        }
    }
}
