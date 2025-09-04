package com.team5.catdogeats.notifications.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Deque;

public interface SseEmitterService {
    SseEmitter connect(String provider, String providerId);
    Deque<SseEmitter> getEmitters(String userId);
    void removeEmitter(String userId, SseEmitter emitter);
}
