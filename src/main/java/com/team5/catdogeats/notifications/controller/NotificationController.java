package com.team5.catdogeats.notifications.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.notifications.service.NotificationCommandService;
import com.team5.catdogeats.notifications.service.NotificationService;
import com.team5.catdogeats.notifications.service.SseEmitterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final NotificationCommandService commandService;

    @GetMapping("/connect")
    public SseEmitter connect(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            return emitterService.connect(userPrincipal.provider(), userPrincipal.providerId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody String message) {
        try {
            notificationService.sendNotification(userPrincipal.provider(), userPrincipal.providerId(), message);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }

    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String notificationId, @AuthenticationPrincipal UserPrincipal principal) {
        try {
            commandService.markAsRead(principal.provider(), principal.providerId(), notificationId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }

    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@AuthenticationPrincipal UserPrincipal principal) {
        try {
            commandService.markAllAsRead(principal.provider(), principal.providerId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }


    }
}
