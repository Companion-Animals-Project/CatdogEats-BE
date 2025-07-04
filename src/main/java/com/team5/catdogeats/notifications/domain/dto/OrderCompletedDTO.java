package com.team5.catdogeats.notifications.domain.dto;

public record OrderCompletedDTO(String orderId,
                                Long orderNumber,
                                String userId,
                                long totalPrice) {
}
