package com.team5.catdogeats.orders.dto.response;

import java.time.LocalDateTime;

/**
 * 주문 내역 삭제(숨김) 응답 DTO
 * API: DELETE /v1/buyers/orders/{orderNumber}
 * 단일 주문의 숨김 처리 결과를 제공
 *
 * @param success 삭제 성공 여부
 * @param orderNumber 처리된 주문 번호
 * @param orderId 처리된 주문 ID
 * @param hiddenAt 숨김 처리 시각 (성공한 경우에만)
 * @param message 처리 결과 메시지
 */
public record OrderDeleteResponse(

        boolean success,
        Long orderNumber,
        String orderId,
        LocalDateTime hiddenAt,
        String message

) {

    // Record의 Compact Constructor - 검증 및 기본값 설정
    public OrderDeleteResponse {
        // 메시지가 null인 경우 기본 메시지 설정
        if (message == null || message.trim().isEmpty()) {
            if (success) {
                message = "주문 내역이 성공적으로 삭제되었습니다.";
            } else {
                message = "주문 내역 삭제에 실패했습니다.";
            }
        }

        // 실패한 경우 hiddenAt은 null로 설정
        if (!success) {
            hiddenAt = null;
        }
    }

    /**
     * 성공 응답 생성을 위한 정적 팩토리 메서드
     * @param orderNumber 주문 번호
     * @param orderId 주문 ID
     * @param hiddenAt 숨김 처리 시각
     * @return 성공 응답
     */
    public static OrderDeleteResponse success(Long orderNumber, String orderId, LocalDateTime hiddenAt) {
        return new OrderDeleteResponse(
                true,
                orderNumber,
                orderId,
                hiddenAt,
                "주문 내역이 성공적으로 삭제되었습니다."
        );
    }

    /**
     * 실패 응답 생성을 위한 정적 팩토리 메서드
     * @param orderNumber 주문 번호
     * @param errorMessage 실패 사유
     * @return 실패 응답
     */
    public static OrderDeleteResponse failure(Long orderNumber, String errorMessage) {
        return new OrderDeleteResponse(
                false,
                orderNumber,
                null,
                null,
                errorMessage
        );
    }

    /**
     * 간단한 성공 응답 생성 (orderId 없이)
     * @param orderNumber 주문 번호
     * @param hiddenAt 숨김 처리 시각
     * @return 성공 응답
     */
    public static OrderDeleteResponse success(Long orderNumber, LocalDateTime hiddenAt) {
        return success(orderNumber, null, hiddenAt);
    }
}