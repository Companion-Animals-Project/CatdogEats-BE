package com.team5.catdogeats.orders.dto.common;

public record OrderItemSnapshot(String productId,
                                String productName,
                                Integer quantity,
                                Long unitPrice,
                                Long totalPrice,
                                String sellerId) {
}
