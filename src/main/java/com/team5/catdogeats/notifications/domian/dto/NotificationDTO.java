package com.team5.catdogeats.notifications.domian.dto;

import com.team5.catdogeats.notifications.domian.enums.NotificationType;

import java.time.ZonedDateTime;

public record NotificationDTO(String title,
                              String message,
                              NotificationType type,
                              ZonedDateTime createdAt) {


}
