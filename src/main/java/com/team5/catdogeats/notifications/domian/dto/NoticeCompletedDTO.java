package com.team5.catdogeats.notifications.domian.dto;

import com.team5.catdogeats.notifications.domian.enums.NotificationType;

public record NoticeCompletedDTO(
                                 String title,
                                 String message,
                                 NotificationType type) {
}
