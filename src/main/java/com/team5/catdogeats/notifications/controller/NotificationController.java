package com.team5.catdogeats.notifications.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.notifications.service.NotificationService;
import com.team5.catdogeats.notifications.service.SseEmitterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1/users/notifications")
@RequiredArgsConstructor
@Tag(name = "알림", description = "알림설정에 관여하는 API입니다.")
public class NotificationController {
    private final SseEmitterService emitterService;
    private final NotificationService notificationService;

    @GetMapping("/connect")
    public SseEmitter connect(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return emitterService.connect(userPrincipal.provider(), userPrincipal.providerId());
    }

    @PostMapping("/send")
    public ResponseEntity<Void> send(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody String message) {
        notificationService.sendNotification(userPrincipal.provider(), userPrincipal.providerId(), message);
        return ResponseEntity.ok().build();
    }
}
