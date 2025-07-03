package com.team5.catdogeats.notifications.service.impl;

import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.notifications.service.NotificationService;
import com.team5.catdogeats.notifications.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final SseEmitterService emitterService;
    private final UserIdCacheService userIdCacheService;

    @Override
    @Async("SSE")
    public void sendNotification(String provider, String providerId, String message) {

        String userId = getUserId(provider, providerId);
        List<SseEmitter> emitters = emitterService.getEmitters(userId);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(message));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
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
