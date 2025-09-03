package com.team5.catdogeats.notifications.service.impl;

import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.notifications.service.SseEmitterService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class SseEmitterServiceImpl implements SseEmitterService {
    private final MeterRegistry meterRegistry;
    private final UserIdCacheService userIdCacheService;
    private final Map<String, Deque<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private static final Long DEFAULT_TIMEOUT = 30 * 60 * 1000L;
    private static final int MAX_PER_USER = 5;


    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public SseEmitterServiceImpl(MeterRegistry meterRegistry, UserIdCacheService userIdCacheService) {
        this.meterRegistry = meterRegistry;
        this.userIdCacheService = userIdCacheService;

        // Gauge 한 번 등록
        meterRegistry.gauge("sse_active_connections", activeConnections);
    }
    @Override
    public SseEmitter connect(String provider, String providerId) {
        String userId = getUserId(provider, providerId);

        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        Deque<SseEmitter> deque = emitters.computeIfAbsent(userId, k -> new ConcurrentLinkedDeque<>());
        while (deque.size() >= MAX_PER_USER) {
            SseEmitter oldest = deque.pollFirst(); // 오래된 연결 제거
            if (oldest != null) {
                try {
                    oldest.complete();
                } catch (Exception ex) {
                    log.debug("oldest.complete() failed for user={}, err={}", userId, ex.getMessage());
                } finally {
                    activeConnections.decrementAndGet();
                }

            }
        }

        deque.addLast(emitter);
        activeConnections.incrementAndGet(); // 전역 카운트 증가

        Runnable cleanup = () -> {
            removeEmitterAndDecrement(userId, emitter);
        };

        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());


        // 연결 즉시 확인 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("연결되었습니다"));
        } catch (IOException e) {
            cleanup.run();
            emitter.completeWithError(e);
            return emitter;
        }

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
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    @Override
    public Deque<SseEmitter> getEmitters(String userId) {
        return emitters.getOrDefault(userId, new ArrayDeque<>());
    }


    @Override
    public void removeEmitter(String userId, SseEmitter emitter) {
        Deque<SseEmitter> deque = emitters.get(userId);
        if (deque != null) {
            deque.remove(emitter);
            if (deque.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }

    private void removeEmitterAndDecrement(String userId, SseEmitter emitter) {
        Deque<SseEmitter> deque = emitters.get(userId);
        if (deque == null) {
            return;
        }
        boolean removed = deque.remove(emitter);
        if (removed) {
            // if deque became empty, remove the key
            if (deque.isEmpty()) {
                emitters.remove(userId, deque);
            }
            int now = activeConnections.decrementAndGet();
            log.debug("Emitter removed for userId={}, remainingGlobalConnections={}", userId, now);
        } else {
            log.debug("Emitter to remove not found for userId={}, maybe already removed", userId);
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
