package com.team5.catdogeats.orders.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 주문 내역 삭제 요청 DTO
 * API: DELETE /v1/buyers/orders
 * 구매자가 특정 주문을 숨김 처리할 때 사용
 *
 * @param orderNumber 삭제할 주문 번호 (필수, 양수)
 */
public record OrderDeleteRequest(
        @NotBlank(message = "주문 번호는 필수입니다.")
        String orderNumber
) {
    // String 타입으로 변경되었으므로 숫자 비교 로직을 제거합니다.
    // @NotBlank 어노테이션이 null 또는 공백 문자열을 방지해줍니다.
}