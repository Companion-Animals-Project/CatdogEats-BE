package com.team5.catdogeats.notifications.service.impl;

import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.notifications.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class SseEmitterServiceImpl implements SseEmitterService {
    private final UserIdCacheService userIdCacheService;
    private final Map<String, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private static final Long DEFAULT_TIMEOUT = 30 * 60 * 1000L;
    private static final Long HEARTBEAT_INTERVAL = 30 * 1000L;
    @Override
    public SseEmitter connect(String provider, String providerId) {
        String userId = getUserId(provider, providerId);

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 즉시 확인 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("연결되었습니다"));
        } catch (IOException e) {
            emitters.get(userId).remove(emitter);
            emitter.completeWithError(e);
            return emitter;
        }

        // 정리 로직
        Runnable cleanup = () -> {
            List<SseEmitter> userEmitters = emitters.get(userId);
            if (userEmitters != null) {
                userEmitters.remove(emitter);
                if (userEmitters.isEmpty()) {
                    emitters.remove(userId);
                }
            }
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    @Scheduled(fixedRate = 30 * 1000L)
    public void sendHeartbeats() {
        emitters.forEach((userId, emitterList) -> {
            for (SseEmitter emitter : emitterList) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("ping")
                            .data("heartbeat"));
                } catch (IOException e) {
                    emitter.completeWithError(e);
                    emitterList.remove(emitter);
                }
            }
        });
    }


    @Override
    public List<SseEmitter> getEmitters(String userId) {
        return emitters.getOrDefault(userId, Collections.emptyList());
    }

    private String getUserId(String provider, String providerId) {
        String userId = userIdCacheService.getCachedUserId(provider, providerId);

        if (userId == null) {
            userIdCacheService.cacheUserIdAndRole(provider, providerId);
            userId = userIdCacheService.getCachedUserId(provider, providerId);
        }
        return userId;
    }

    @Override
    public void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

}
