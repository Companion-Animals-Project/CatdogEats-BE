package com.team5.catdogeats.outbox.util;

import com.team5.catdogeats.outbox.domain.ProcessedMessage;
import com.team5.catdogeats.outbox.repository.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotentConsumer {

    private final ProcessedMessageRepository processedMessageRepository;

    public boolean processOnce(String messageId, String consumerGroup) {
        if (messageId == null || messageId.isEmpty()) {
            log.warn("메시지 ID가 없습니다. 중복 처리 방지를 위해 메시지를 처리합니다.");
            return true;  // 메시지 ID가 없으면 처리 진행 (일단 안전하게)
        }

        try {
            boolean exists = processedMessageRepository.existsByMessageIdAndConsumerGroup(messageId, consumerGroup);
            if (exists) {
                log.debug("이미 처리된 메시지입니다: messageId={}, consumerGroup={}", messageId, consumerGroup);
                return false;
            }

            ProcessedMessage processedMessage = ProcessedMessage.builder()
                    .messageId(messageId)
                    .processedAt(ZonedDateTime.now())
                    .consumerGroup(consumerGroup)
                    .build();

            processedMessageRepository.save(processedMessage);
            return true;  // 저장 성공 = 처음 처리하는 메시지

        } catch (Exception e) {
            // 중복 키 예외는 이미 처리된 메시지를 의미
            log.debug("메시지가 이미 처리되었습니다: messageId={}, consumerGroup={}",
                    messageId, consumerGroup);
            return false;
        }
    }
}

