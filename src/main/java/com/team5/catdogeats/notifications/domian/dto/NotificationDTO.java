package com.team5.catdogeats.notifications.domian.dto;

import com.team5.catdogeats.notifications.domian.enums.NotificationType;

public record NotificationDTO(String id,
                              String title,
                              String message,
                              NotificationType type,
                              String createdAt) {
}
