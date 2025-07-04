package com.team5.catdogeats.orders.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 주문 내역 삭제 요청 DTO
 * API: DELETE /v1/buyers/orders
 * 구매자가 특정 주문을 숨김 처리할 때 사용
 *
 * @param orderNumber 삭제할 주문 번호 (필수, 양수)
 */
public record OrderDeleteRequest(

        @NotNull(message = "주문 번호는 필수입니다.")
        @Positive(message = "주문 번호는 양수여야 합니다.")
        Long orderNumber

) {

    // 생성자에서 추가 검증 수행
    public OrderDeleteRequest {
        if (orderNumber != null && orderNumber <= 0) {
            throw new IllegalArgumentException("주문 번호는 양수여야 합니다.");
        }
    }
}