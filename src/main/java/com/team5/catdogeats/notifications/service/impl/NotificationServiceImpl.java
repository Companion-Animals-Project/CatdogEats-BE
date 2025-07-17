package com.team5.catdogeats.notifications.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserIdCacheService userIdCacheService;
    private final ObjectMapper objectMapper;

    @Override
    @Async("SSE")
    public void sendNotification(String provider, String providerId, Object message) {
        try {
            String userId = getUserId(provider, providerId);

            // Redis를 통해 알림 발송 (SSE 연결된 사용자들만 받음)
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend("notify:" + userId, json);

            log.info("Redis를 통해 알림 발송 완료: userId={}", userId);
        } catch (Exception e) {
            log.error("Redis 알림 발송 중 오류 발생: provider={}, providerId={}, error={}",
                    provider, providerId, e.getMessage(), e);
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
