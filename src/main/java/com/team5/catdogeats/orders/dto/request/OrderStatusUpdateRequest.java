package com.team5.catdogeats.orders.dto.request;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문 상태 변경 요청 DTO
 * API: POST /v1/sellers/orders/status
 *
 * 판매자가 본인이 판매한 상품이 포함된 주문의 상태를 변경할 때 사용하는 요청 구조
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class OrderStatusUpdateRequest {

    /**
     * 상태를 변경할 주문 번호
     */
    @NotBlank(message = "주문 번호는 필수입니다")
    private String orderNumber;

    /**
     * 변경할 주문 상태
     * 허용되는 상태 전환:
     * - PAYMENT_COMPLETED → PREPARING (상품준비중)
     * - PREPARING → READY_FOR_SHIPMENT (배송준비완료)
     * - PREPARING → CANCELLED (주문취소) - 특별한 경우
     * - READY_FOR_SHIPMENT → IN_DELIVERY (배송중) - 운송장 등록 시에만
     */
    @NotNull(message = "변경할 주문 상태는 필수입니다")
    private OrderStatus newStatus;

    /**
     * 상태 변경 사유 (선택사항)
     * 특정 상황에서 필수:
     * - CANCELLED 상태로 변경 시: 취소 사유 필수
     * - 출고 지연 발생 시: 지연 사유 필수
     */
    @Size(max = 500, message = "상태 변경 사유는 500자를 초과할 수 없습니다")
    private String reason;

    /**
     * 출고 지연 여부
     * true인 경우 reason 필드에 지연 사유가 반드시 포함되어야 함
     */
    @Builder.Default
    private Boolean isDelayed = false;

    /**
     * 예상 출고일 (출고 지연 시에만 사용)
     * yyyy-MM-dd 형식으로 전달
     */
    private String expectedShipDate;

    /**
     * 취소 상태로 변경하는 요청인지 확인
     * @return 취소 요청 여부
     */
    public boolean isCancellationRequest() {
        return OrderStatus.CANCELLED.equals(newStatus);
    }

    /**
     * 배송 시작 상태로 변경하는 요청인지 확인
     * @return 배송 시작 요청 여부
     */
    public boolean isShipmentStartRequest() {
        return OrderStatus.IN_DELIVERY.equals(newStatus);
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
     * 유효한 상태 전환인지 검증
     * @param currentStatus 현재 주문 상태
     * @return 유효한 전환 여부
     */
    public boolean isValidStatusTransition(OrderStatus currentStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        // 동일한 상태로의 변경은 불가
        if (currentStatus.equals(newStatus)) {
            return false;
        }

        return switch (currentStatus) {
            case PAYMENT_COMPLETED ->
                    newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.CANCELLED;
            case PREPARING ->
                    newStatus == OrderStatus.READY_FOR_SHIPMENT || newStatus == OrderStatus.CANCELLED;
            case READY_FOR_SHIPMENT ->
                    newStatus == OrderStatus.IN_DELIVERY || newStatus == OrderStatus.CANCELLED;
            case IN_DELIVERY ->
                    newStatus == OrderStatus.DELIVERED; // 자동 처리되므로 수동 변경 불가
            default -> false; // 기타 상태에서는 변경 불가
        };
    }
}