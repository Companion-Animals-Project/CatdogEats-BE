package com.team5.catdogeats.notifications.domain.dto;

import com.team5.catdogeats.notifications.domain.enums.NotificationType;

public record OrderCompletedDTO(String orderId,
                                String orderNumber,
                                String userId,
                                long totalPrice,
                                NotificationType type) {
}
