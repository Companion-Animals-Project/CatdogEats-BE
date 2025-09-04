package com.team5.catdogeats.notifications.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.team5.catdogeats.notifications.domain.enums.NotificationType;

import java.time.ZonedDateTime;

public record NotificationDTO(String title,
                              String message,
                              @JsonProperty("type") NotificationType type,
                              ZonedDateTime createdAt) {


}
