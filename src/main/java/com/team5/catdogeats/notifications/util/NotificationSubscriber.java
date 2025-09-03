package com.team5.catdogeats.notifications.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.notifications.domain.dto.NoticeCompletedDTO;
import com.team5.catdogeats.notifications.domain.dto.OrderCompletedDTO;
import com.team5.catdogeats.notifications.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Deque;

@Slf4j
@Component
@RequiredArgsConstructor

public class NotificationSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final SseEmitterService sseEmitterService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(Message redisMsg, byte[] pattern) {


        String channel = new String(redisMsg.getChannel(), StandardCharsets.UTF_8);
//        String messageBody = new String(redisMsg.getBody());
        String userId = channel.substring("notify:".length());

        try {
            Object messageObj = redisTemplate.getValueSerializer().deserialize(redisMsg.getBody());
            if (messageObj == null) {
                log.warn("Redis 메시지 null: 채널={}", channel);
                return;
            }

            String messageBody;
            if (messageObj instanceof String str) {
                messageBody = str; // 이미 JSON 문자열이면 그대로 사용
            } else {
                messageBody = objectMapper.writeValueAsString(messageObj); // 객체면 JSON으로 변환
            }            // 해당 사용자에게 연결된 SSE 연결들에 알림 전송
            Object notificationData = parseNotificationMessage(messageBody);
            if (notificationData != null) { // null 체크 추가
                sendNotificationToUser(userId, notificationData); // ✅ 호출 추가
            }

            log.debug("Redis → SSE 전송 완료: user={}, data={}", userId, notificationData);

        } catch (Exception e) {
            log.error("알림 메시지 처리 실패: {}",  e.getMessage());
        }
    }

    private Object parseNotificationMessage(String messageBody) {
        try {
            JsonNode root = objectMapper.readTree(messageBody);
            JsonNode typeNode = root.get("type");
            if (typeNode == null || typeNode.isNull()) {
                log.warn("type 필드 없음, 메시지 무시: {}", messageBody);
                return null;
            }
            String type = typeNode.asText();

            return switch (type) {
                case "NOTICE" -> objectMapper.readValue(messageBody, NoticeCompletedDTO.class);
                case "NEW_ORDER" -> objectMapper.readValue(messageBody, OrderCompletedDTO.class);
                case "CHATTING" -> objectMapper.readValue(messageBody, NoticeCompletedDTO.class);
                default -> throw new IllegalStateException("지원하지 않는 알림입니다.");
            };

        } catch (Exception e) {
            log.error("json 파싱 중 에러 {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void sendNotificationToUser(String userId, Object notificationData) {
        Deque<SseEmitter> emitters = sseEmitterService.getEmitters(userId);

        if (emitters.isEmpty()) {
            log.debug("사용자 {}에 대한 SSE 연결이 없어 알림을 전송하지 않습니다.", userId);
            return;
        }

        log.info("사용자 {}에게 알림 전송 시도: {} 개의 연결", userId, emitters.size());

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notificationData));

                log.debug("사용자 {}에게 알림 전송 완료", userId);
            } catch (IOException e) {
                log.warn("사용자 {}에 대한 SSE 연결 오류로 연결 제거: {}", userId, e.getMessage());
                emitter.completeWithError(e);
                sseEmitterService.removeEmitter(userId, emitter);
            } catch (Exception e) {
                log.error("사용자 {}에 대한 알림 전송 중 예외 발생: {}", userId, e.getMessage(), e);
                sseEmitterService.removeEmitter(userId, emitter);
            }
        }
    }

}
