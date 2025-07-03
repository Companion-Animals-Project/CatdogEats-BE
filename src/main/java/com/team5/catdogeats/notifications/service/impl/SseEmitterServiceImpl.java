package com.team5.catdogeats.notifications.service.impl;

import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.notifications.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final static Long MAX_CONNECT = 60  * 1000L;

    @Override
    public SseEmitter connect(String provider, String providerId) {
        String userId = getUserId(provider, providerId);

        SseEmitter emitter = new SseEmitter(MAX_CONNECT);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> emitters.get(userId).remove(emitter));
        emitter.onTimeout(() -> emitters.get(userId).remove(emitter));
        emitter.onError(e -> emitters.get(userId).remove(emitter));

        return emitter;
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
}
