package com.team5.catdogeats.notifications.util;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper mapper;
    private final SseEmitterService emitterService;

    @Override
    public void onMessage(Message redisMsg, byte[] pattern) {


        String channel = new String(redisMsg.getChannel(), StandardCharsets.UTF_8); // "notify:123"
        String userId = channel.replace("notify:", "");
        String json = new String(redisMsg.getBody(), StandardCharsets.UTF_8);

        try {
            NotificationDTO dto = mapper.readValue(json, NotificationDTO.class);
            List<SseEmitter> emitters = emitterService.getEmitters(userId);

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .id(dto.id())
                            .name("notification")
                            .data(dto));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }

            log.debug("Redis → SSE 전송 완료: user={}, data={}", userId, dto);
        } catch (Exception e) {
            log.error("알림 메시지 처리 실패: {}", json, e);
        }
    }

}
