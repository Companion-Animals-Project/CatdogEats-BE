package com.team5.catdogeats.carts.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@Builder
public class CartItemResponse {
    private String id;
    private String productId;
    private String productName;
    private String productImage;
    private Long productPrice;
    private int quantity;
    private Long totalPrice; // price * quantity
    private ZonedDateTime addedAt;

    // 프론트엔드 UI 제어용 필드
    private boolean canIncrease;  // 수량 증가 가능 여부 (quantity < 10)
    private boolean canDecrease;  // 수량 감소 가능 여부 (quantity > 1)
    private int maxQuantity;      // 최대 수량 정보 (10)
    private int minQuantity;      // 최소 수량 정보 (1)

    // CartItems 엔티티로부터 Response 생성
    public static CartItemResponse from(com.team5.catdogeats.carts.domain.mapping.CartItems cartItem) {
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(cartItem.getProduct().getId())
                .productName(cartItem.getProduct().getTitle())
                .productImage("") // S3 로직 완성 후 추가
                .productPrice(cartItem.getProduct().getPrice())
                .quantity(cartItem.getQuantity())
                .totalPrice(cartItem.getProduct().getPrice() * cartItem.getQuantity())
                .addedAt(cartItem.getAddedAt())
                // UI 제어 정보
                .canIncrease(cartItem.getQuantity() < 10)
                .canDecrease(cartItem.getQuantity() > 1)
                .maxQuantity(10)
                .minQuantity(1)
                .build();
    }
}
