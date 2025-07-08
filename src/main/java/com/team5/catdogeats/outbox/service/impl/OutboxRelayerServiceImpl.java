package com.team5.catdogeats.outbox.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.global.config.RabbitMQConfig;
import com.team5.catdogeats.outbox.domain.OutboxMessage;
import com.team5.catdogeats.outbox.domain.ProcessedMessage;
import com.team5.catdogeats.outbox.domain.dto.OutboxEventData;
import com.team5.catdogeats.outbox.domain.enums.OutboxStatus;
import com.team5.catdogeats.outbox.repository.OutboxMessageRepository;
import com.team5.catdogeats.outbox.repository.ProcessedMessageRepository;
import com.team5.catdogeats.outbox.service.OutboxRelayerService;
import io.debezium.engine.RecordChangeEvent;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.data.Field;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelayerServiceImpl implements OutboxRelayerService {
    private final OutboxMessageRepository outboxMessageRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final int BATCH_SIZE = 100;
    private final List<String> batchedMessageIds = new ArrayList<>(BATCH_SIZE);

    @PreDestroy
    public void flushRemaining() {
        // 아직 DB에 반영되지 않은 ID가 있으면 한 번 더 벌크 업데이트
        batchUpdateStatus();
    }


    @Override
    @JpaTransactional(propagation = Propagation.REQUIRES_NEW)
    public void handleChangeEvent(RecordChangeEvent<SourceRecord> event) {
        SourceRecord record = event.record();

        try {
            // 1. value()에서 CDC 'after' 데이터 추출
            Struct value = (Struct) record.value();
            if (value == null) return;

            Struct after = (Struct) value.get("after");
            if (after == null) return;

            // 2. JSON 문자열로 변환
            String json = structToJson(after);

            log.debug("CDC 이벤트 수신: {}", json);

            // 3. OutboxEventData로 역직렬화
            OutboxEventData eventData = objectMapper.readValue(json, OutboxEventData.class);


            // 중복 처리 방지
            if (processedMessageRepository.existsById(eventData.id())) {
                log.warn("이미 처리된 메시지: {}", eventData.id());
                return;
            }

            // PENDING 상태만 처리
            if (!OutboxStatus.PENDING.toString().equals(eventData.status())) {
                log.warn("PENDING 상태가 아닌 메시지: id={}, eventType={}", eventData.id(), eventData.eventType());
                return;
            }

            // 메시지 발송
            String exchangeName = determineExchangeName(eventData.eventType());
            String routingKey = determineRoutingKey(eventData.eventType());

            MessageProperties props = new MessageProperties();
            props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            props.setMessageId(eventData.id());
            props.setContentType("application/json;charset=UTF-8");
            props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            props.setTimestamp(new Date());

            Message amqpMessage = MessageBuilder
                    .withBody(eventData.payload().getBytes())
                    .andProperties(props)
                    .build();

            rabbitTemplate.send(exchangeName, routingKey, amqpMessage);

            batchedMessageIds.add(eventData.id());
            batchUpdateStatus();


            processedMessageRepository.save(
                    ProcessedMessage.builder()
                            .messageId(eventData.id())
                            .processedAt(ZonedDateTime.now())
                            .consumerGroup("default-group") // 필요 시 외부 주입
                            .build()
            );

            log.info("CDC 메시지 발송 성공: id={}, 타입={}", eventData.id(), eventData.eventType());

        } catch (Exception e) {
            log.error("CDC 이벤트 처리 실패: {}", e.getMessage(), e);
            handleProcessingFailure(event.record(), e);
        }
    }


    private String structToJson(Struct struct) throws Exception {
        Map<String, Object> map = new HashMap<>();
        for (Field field : struct.schema().fields()) {
            map.put(field.name(), struct.get(field));
        }
        return objectMapper.writeValueAsString(map);
    }


    private void handleProcessingFailure(SourceRecord record, Exception e) {
        try {
            // 1. 메시지 ID 추출 (key 또는 value 구조에서 가져옴)
            String messageId = extractMessageIdFromRecord(record);
            if (messageId == null) {
                log.error("메시지 ID 추출 실패: record key={}", record.key());
                return;
            }

            // 2. 메시지 조회 및 실패 마킹
            OutboxMessage message = outboxMessageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("메시지 없음: " + messageId));

            message.markAsFailed(e.getMessage());

            // 3. DLQ 조건 확인 및 전송
            if (message.getRetryCount() >= 3) {
                rabbitTemplate.convertAndSend(
                        "dlx.order.events",
                        message.getEventType() + ".dlq",
                        message.getPayload());

                log.warn("DLQ 전송 완료: id={}, 타입={}, 재시도={}",
                        message.getId(), message.getEventType(), message.getRetryCount());
            }

            // 4. 상태 저장
            outboxMessageRepository.save(message);

        } catch (Exception ex) {
            log.error("🚨 DLQ 처리 중 중첩 오류: {}", ex.getMessage(), ex);
        }
    }

    private String extractMessageIdFromRecord(SourceRecord record) {
        try {
            Struct value = (Struct) record.value();
            if (value == null) return null;

            Struct after = (Struct) value.get("after");
            if (after == null) return null;
            log.debug("value.after: {}", after);
            return after.getString("id");
        } catch (Exception e) {
            log.debug("value.after: {}", record.value());
            log.error("record에서 messageId 추출 실패: {}", e.getMessage(), e);
            return null;
        }
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

    private void batchUpdateStatus() {

        while (batchedMessageIds.size() >= BATCH_SIZE) { // Debezium이 backlog를 몰아서 줄 때 필요함
            List<String> ids = new ArrayList<>(batchedMessageIds.subList(0, BATCH_SIZE));
            outboxMessageRepository.updateStatusForBatch(ids, OutboxStatus.SENT, ZonedDateTime.now());
            batchedMessageIds.removeAll(ids);
        }

        if (!batchedMessageIds.isEmpty()) {
            outboxMessageRepository.updateStatusForBatch(
                    new ArrayList<>(batchedMessageIds),
                    OutboxStatus.SENT,
                    ZonedDateTime.now());
            batchedMessageIds.clear();
        }
    }


}
