package com.team5.catdogeats.notifications.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.notifications.domian.dto.NoticeCompletedDTO;
import com.team5.catdogeats.notifications.domian.dto.NotificationDTO;
import com.team5.catdogeats.notifications.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor

public class NotificationSubscriber implements MessageListener {
    private final ObjectMapper objectMapper;
    private final SseEmitterService sseEmitterService;

    @Override
    public void onMessage(Message redisMsg, byte[] pattern) {


        String channel = new String(redisMsg.getChannel(), StandardCharsets.UTF_8);
        String messageBody = new String(redisMsg.getBody());
        String userId = channel.substring("notify:".length());

        try {
            Object notificationData = parseNotificationMessage(messageBody);

            // 해당 사용자에게 연결된 SSE 연결들에 알림 전송
            sendNotificationToUser(userId, notificationData);
            log.debug("Redis → SSE 전송 완료: user={}, data={}", userId, notificationData);

        } catch (Exception e) {
            log.error("알림 메시지 처리 실패: {}",  e.getMessage());
        }
    }

    private Object parseNotificationMessage(String messageBody) {
        try {
            // 먼저 NotificationDTO로 파싱 시도
            log.debug("첫 번째 파싱");
            return objectMapper.readValue(messageBody, NotificationDTO.class);
        } catch (Exception e1) {
            try {
                // NotificationDTO 파싱 실패 시 NoticeCompletedDTO로 시도
                log.debug("두번째 파싱");
                return objectMapper.readValue(messageBody, NoticeCompletedDTO.class);
            } catch (Exception e2) {
                // 둘 다 실패하면 원본 문자열 반환
                log.warn("JSON 파싱 실패, 원본 문자열 전송: {}", messageBody);
                return messageBody;
            }
        }
    }

    private void sendNotificationToUser(String userId, Object notificationData) {
        List<SseEmitter> emitters = sseEmitterService.getEmitters(userId);

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
