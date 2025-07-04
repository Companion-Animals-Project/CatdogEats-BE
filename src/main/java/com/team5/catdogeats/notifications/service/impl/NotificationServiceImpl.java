package com.team5.catdogeats.notifications.service.impl;

import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.notifications.domian.dto.NotificationDTO;
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

    @Override
    @Async("SSE")
    public void sendNotification(String provider, String providerId, Object message) {
        try {
            String userId = getUserId(provider, providerId);

            // Redis를 통해 알림 발송 (SSE 연결된 사용자들만 받음)
            redisTemplate.convertAndSend("notify:" + userId, message);

            log.info("Redis를 통해 알림 발송 완료: userId={}", userId);
        } catch (Exception e) {
            log.error("Redis 알림 발송 중 오류 발생: provider={}, providerId={}, error={}",
                    provider, providerId, e.getMessage(), e);
        }
    }

    // NotificationDTO를 받는 새로운 메서드 추가
    @Async("SSE")
    public void sendNotificationDTO(String provider, String providerId, NotificationDTO notificationDTO) {
        try {
            String userId = getUserId(provider, providerId);

            // Redis를 통해 알림 발송
            redisTemplate.convertAndSend("notify:" + userId, notificationDTO);

            log.info("Redis를 통해 알림 발송 완료: userId={}, title={}", userId, notificationDTO.title());
        } catch (Exception e) {
            log.error("Redis 알림 발송 중 오류 발생: provider={}, providerId={}, error={}",
                    provider, providerId, e.getMessage(), e);
        }
    }

    @Async("SSE")
    public void sendNotificationByUserId(String userId, NotificationDTO notificationDTO) {
        try {
            // Redis를 통해 알림 발송
            redisTemplate.convertAndSend("notify:" + userId, notificationDTO);

            log.info("Redis를 통해 알림 발송 완료: userId={}, title={}", userId, notificationDTO.title());
        } catch (Exception e) {
            log.error("Redis 알림 발송 중 오류 발생: userId={}, error={}", userId, e.getMessage(), e);
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
