package com.team5.catdogeats.notifications.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.notifications.domain.dto.NotificationReadRequestDTO;
import com.team5.catdogeats.notifications.domain.dto.NotificationResponseDTO;
import com.team5.catdogeats.notifications.domain.dto.NotificationSearchRequestDTO;
import com.team5.catdogeats.notifications.service.NotificationCommandService;
import com.team5.catdogeats.notifications.service.NotificationService;
import com.team5.catdogeats.notifications.service.SseEmitterService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

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
        if (userPrincipal == null) {
            throw new AuthenticationCredentialsNotFoundException("유효하지 않은 토큰");
        }

        try {
            return emitterService.connect(userPrincipal.provider(), userPrincipal.providerId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> send(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody String message) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }
        try {
            notificationService.sendNotification(userPrincipal.provider(), userPrincipal.providerId(), message);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }

    }

    @PatchMapping("/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@RequestBody @Valid NotificationReadRequestDTO request,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }
        try {
            commandService.markAsRead(principal, request.notificationId());
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }

    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getNotifications(@RequestBody NotificationSearchRequestDTO dto,
                                                                                      @AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }

        try {
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, commandService.getNotifications(principal, dto, 10)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }

    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countUnreadNotifications(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }
        try {
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, commandService.countUnreadNotifications(principal)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

}
