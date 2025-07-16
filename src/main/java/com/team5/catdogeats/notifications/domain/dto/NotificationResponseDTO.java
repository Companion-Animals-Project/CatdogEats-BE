package com.team5.catdogeats.notifications.domain.dto;

import com.team5.catdogeats.notifications.domain.enums.NotificationType;
import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record NotificationResponseDTO(String id,
                                      String notificationId,
                                      NotificationType notificationType,
                                      String title,
                                      String message,
                                      boolean isRead,
                                      ZonedDateTime readAt,
                                      ZonedDateTime createdAt) {
}
