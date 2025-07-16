package com.team5.catdogeats.notifications.domain.dto;

import com.team5.catdogeats.notifications.domain.enums.NotificationType;

public record NoticeCompletedDTO(String title,
                                 String message,
                                 NotificationType type) {
}
