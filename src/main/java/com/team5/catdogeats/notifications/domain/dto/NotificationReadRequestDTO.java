package com.team5.catdogeats.notifications.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationReadRequestDTO(@NotBlank String notificationId) {
}
