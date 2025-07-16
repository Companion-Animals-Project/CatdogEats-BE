package com.team5.catdogeats.notifications.domain.dto;

import com.team5.catdogeats.notifications.domain.enums.NotificationType;

import java.time.ZonedDateTime;

public record NotificationDTO(String title,
                              String message,
                              NotificationType type,
                              ZonedDateTime createdAt) {


}
