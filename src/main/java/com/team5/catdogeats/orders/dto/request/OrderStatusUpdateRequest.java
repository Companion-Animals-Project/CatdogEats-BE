package com.team5.catdogeats.orders.dto.request;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 주문 상태 변경 요청 DTO
 * API: POST /v1/sellers/orders/status
 * 판매자가 본인이 판매한 상품이 포함된 주문의 상태를 변경할 때 사용하는 요청 구조
 */
public record OrderStatusUpdateRequest(

        /*
         * 상태를 변경할 주문 번호
         */
        @NotBlank(message = "주문 번호는 필수입니다")
        String orderNumber,

        /*
         * 변경할 주문 상태
         * 허용되는 상태 전환:
         * - PAYMENT_COMPLETED → PREPARING (상품준비중)
         * - PREPARING → READY_FOR_SHIPMENT (배송준비완료)
         * - PREPARING → CANCELLED (주문취소) - 특별한 경우
         * - READY_FOR_SHIPMENT → IN_DELIVERY (배송중) - 운송장 등록 시에만
         */
        @NotNull(message = "변경할 주문 상태는 필수입니다")
        OrderStatus newStatus,

        /*
         * 상태 변경 사유 (선택사항)
         * 특정 상황에서 필수:
         * - CANCELLED 상태로 변경 시: 취소 사유 필수
         * - 출고 지연 발생 시: 지연 사유 필수
         */
        @Size(max = 500, message = "상태 변경 사유는 500자를 초과할 수 없습니다")
        String reason,

        /*
         * 출고 지연 여부
         * true인 경우 reason 필드에 지연 사유가 반드시 포함되어야 함
         */
        Boolean isDelayed,

        /*
         * 예상 출고일 (출고 지연 시에만 사용)
         * yyyy-MM-dd 형식으로 전달
         */
        String expectedShipDate
) {

    /**
     * Compact Constructor - 기본값 설정 및 유효성 검증
     */
    public OrderStatusUpdateRequest {
        // 기본값 설정
        if (isDelayed == null) {
            isDelayed = false;
        }

        // 사유 필수 조건 검증
        if (isReasonRequired() && (reason == null || reason.trim().isEmpty())) {
            throw new IllegalArgumentException("해당 상태 변경에는 사유가 필수입니다");
        }

        // 지연 요청 시 사유 필수 검증
        if (Boolean.TRUE.equals(isDelayed) && (reason == null || reason.trim().isEmpty())) {
            throw new IllegalArgumentException("출고 지연 시 지연 사유는 필수입니다");
        }
    }

    /**
     * 취소 상태로 변경하는 요청인지 확인
     * @return 취소 요청 여부
     */
    public boolean isCancellationRequest() {
        return OrderStatus.CANCELLED.equals(newStatus);
    }

    /**
     * 출고 지연 요청인지 확인
     * @return 출고 지연 요청 여부
     */
    public boolean isDelayRequest() {
        return Boolean.TRUE.equals(isDelayed);
    }

    /**
     * 사유가 필수인 요청인지 확인
     * @return 사유 필수 여부
     */
    public boolean isReasonRequired() {
        return isCancellationRequest() || isDelayRequest();
    }



    /**
     * 요청 정보 요약 (로깅용)
     * @return 요청 정보 요약
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("주문:%s, 상태:%s", orderNumber, newStatus));

        if (isDelayRequest()) {
            summary.append(", 지연:true");
            if (expectedShipDate != null) {
                summary.append(String.format(", 예상출고:%s", expectedShipDate));
            }
        }

        if (reason != null && !reason.trim().isEmpty()) {
            summary.append(String.format(", 사유:%.20s%s",
                    reason.trim(), reason.trim().length() > 20 ? "..." : ""));
        }

        return summary.toString();
    }

    /**
     * 정적 팩토리 메서드 - 기본 상태 변경
     * @param orderNumber 주문 번호
     * @param newStatus 새 상태
     * @return 상태 변경 요청
     */
    public static OrderStatusUpdateRequest of(String orderNumber, OrderStatus newStatus) {
        return new OrderStatusUpdateRequest(orderNumber, newStatus, null, false, null);
    }
}