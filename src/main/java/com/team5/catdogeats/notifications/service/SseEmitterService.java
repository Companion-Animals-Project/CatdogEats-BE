package com.team5.catdogeats.notifications.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface SseEmitterService {
    SseEmitter connect(String provider, String providerId);
    List<SseEmitter> getEmitters(String userId);
}
