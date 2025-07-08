package com.team5.catdogeats.outbox.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.global.config.RabbitMQConfig;
import com.team5.catdogeats.outbox.domain.OutboxMessage;
import com.team5.catdogeats.outbox.repository.OutboxMessageRepository;
import com.team5.catdogeats.outbox.service.OutboxRelayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayerServiceImpl implements OutboxRelayerService {
    private final OutboxMessageRepository outboxMessageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRY_COUNT = 3;


    @Override
    @Scheduled(fixedDelay = 100)
    @JpaTransactional
    public void processOutboxMessages() {
        List<OutboxMessage> pendingMessages = outboxMessageRepository.findPendingMessages(BATCH_SIZE);
        log.debug("Outbox 메시지 처리 시작: {} 메시지 발견", pendingMessages.size());

        for (OutboxMessage message : pendingMessages) {
            try {
                // 이벤트 타입에 따라 적절한 라우팅 키와 교환기 선택
                String exchangeName = determineExchangeName(message.getEventType());
                String routingKey = determineRoutingKey(message.getEventType());
                log.debug("메시지 발송 시도: id={}, eventType={}, exchange={}, routingKey={}",
                        message.getId(), message.getEventType(), exchangeName, routingKey);

                // RabbitMQ 메시지 생성 및 발송
                MessageProperties props = new MessageProperties();
                props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                props.setMessageId(message.getId());
                props.setTimestamp(Date.from(ZonedDateTime.now().toInstant()));

                Message amqpMessage = MessageBuilder
                        .withBody(message.getPayload().getBytes())
                        .andProperties(props)
                        .build();

                rabbitTemplate.send(exchangeName, routingKey, amqpMessage);

                // 발송 성공으로 상태 변경
                message.markAsSent();
                outboxMessageRepository.save(message);
                log.debug("Outbox 메시지 발송 성공: id={}, 타입={}",
                        message.getId(), message.getEventType());

            } catch (AmqpException e) {
                // 발송 실패 처리 및 재시도 로직
                handleSendFailure(message, e);
            } catch (Exception e) {
                log.error("Outbox 메시지 처리 중 예상치 못한 오류: id={}, 오류={}",
                        message.getId(), e.getMessage(), e);
                message.markAsFailed("내부 오류: " + e.getMessage());
                outboxMessageRepository.save(message);
            }
        }

    }
    private void handleSendFailure(OutboxMessage message, Exception e) {
        message.markAsFailed(e.getMessage());

        if (message.getRetryCount() >= MAX_RETRY_COUNT) {
            // DLQ로 이동시키는 로직 (실패한 메시지를 별도 저장)
            try {
                rabbitTemplate.convertAndSend(
                        "dlx.order.events",
                        message.getEventType() + ".dlq",
                        message.getPayload());
                log.warn("최대 재시도 횟수 초과로 DLQ로 이동: id={}, 타입={}, 재시도={}",
                        message.getId(), message.getEventType(), message.getRetryCount());
            } catch (Exception dlqEx) {
                log.error("DLQ 전송 실패: id={}, 오류={}",
                        message.getId(), dlqEx.getMessage(), dlqEx);
            }
        } else {
            log.warn("Outbox 메시지 발송 실패 (재시도 예정): id={}, 타입={}, 재시도={}",
                    message.getId(), message.getEventType(), message.getRetryCount());
        }

        outboxMessageRepository.save(message);
    }

    private String determineExchangeName(String eventType) {
        if (eventType.startsWith("order.")) {
            return "order.events";
        } else if (eventType.startsWith("payment.")) {
            return "payment.events";
        }
        return "order.events";  // 기본값
    }

    private String determineRoutingKey(String eventType) {
        if (eventType.startsWith("order.created")) {
            return RabbitMQConfig.RK_ORDER_CREATED;
        } else if (eventType.startsWith("payment.completed")) {
            return RabbitMQConfig.RK_PAYMENT_SUCCESS;
        } else if (eventType.startsWith("payment.failed")) {
            return RabbitMQConfig.RK_PAYMENT_FAILED;
        }
        return eventType;
    }


}
