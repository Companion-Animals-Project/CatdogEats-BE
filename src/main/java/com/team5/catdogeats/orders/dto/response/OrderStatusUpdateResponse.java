package com.team5.catdogeats.orders.dto.response;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * 주문 상태 변경 응답 DTO
 * API: POST /v1/sellers/orders/status
 *
 * 판매자가 주문 상태를 변경한 결과를 반환하는 응답 구조
 */
@Builder
public record OrderStatusUpdateResponse(
        String orderNumber,         // 주문 번호
        OrderStatus previousStatus, // 이전 주문 상태
        OrderStatus newStatus,      // 변경된 주문 상태
        String reason,              // 상태 변경 사유
        ZonedDateTime updatedAt,    // 상태 변경 시각
        String message,             // 처리 결과 메시지
        StatusChangeInfo statusChangeInfo // 상태 변경 상세 정보
) {

    /**
     * 상태 변경 상세 정보
     */
    @Builder
    public record StatusChangeInfo(
            Boolean isDelayed,           // 출고 지연 여부
            String expectedShipDate,     // 예상 출고일 (지연 시)
            Boolean requiresTracking,    // 운송장 등록 필요 여부
            String nextStepDescription   // 다음 단계 안내
    ) {}

    /**
     * 성공 응답 생성 - 기본
     * @param orderNumber 주문 번호
     * @param previousStatus 이전 상태
     * @param newStatus 새 상태
     * @param reason 변경 사유
     * @return 주문 상태 변경 응답 DTO
     */
    public static OrderStatusUpdateResponse success(
            String orderNumber,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            String reason
    ) {
        return OrderStatusUpdateResponse.builder()
                .orderNumber(orderNumber)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .updatedAt(ZonedDateTime.now())
                .message(generateSuccessMessage(previousStatus, newStatus))
                .statusChangeInfo(generateStatusChangeInfo(newStatus))
                .build();
    }

    /**
     * 성공 응답 생성 - 상세 정보 포함
     * @param orderNumber 주문 번호
     * @param previousStatus 이전 상태
     * @param newStatus 새 상태
     * @param reason 변경 사유
     * @param isDelayed 출고 지연 여부
     * @param expectedShipDate 예상 출고일
     * @return 주문 상태 변경 응답 DTO
     */
    public static OrderStatusUpdateResponse successWithDelay(
            String orderNumber,
            OrderStatus previousStatus,
            OrderStatus newStatus,
            String reason,
            boolean isDelayed,
            String expectedShipDate
    ) {
        StatusChangeInfo statusInfo = StatusChangeInfo.builder()
                .isDelayed(isDelayed)
                .expectedShipDate(expectedShipDate)
                .requiresTracking(newStatus == OrderStatus.READY_FOR_SHIPMENT)
                .nextStepDescription(generateNextStepDescription(newStatus, isDelayed))
                .build();

        return OrderStatusUpdateResponse.builder()
                .orderNumber(orderNumber)
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .reason(reason)
                .updatedAt(ZonedDateTime.now())
                .message(isDelayed ? "출고 지연이 등록되었습니다" : generateSuccessMessage(previousStatus, newStatus))
                .statusChangeInfo(statusInfo)
                .build();
    }

    /**
     * 성공 메시지 생성
     * @param previousStatus 이전 상태
     * @param newStatus 새 상태
     * @return 성공 메시지
     */
    private static String generateSuccessMessage(OrderStatus previousStatus, OrderStatus newStatus) {
        return switch (newStatus) {
            case PREPARING -> "상품 준비가 시작되었습니다";
            case READY_FOR_SHIPMENT -> "배송 준비가 완료되었습니다. 운송장 번호를 등록해주세요";
            case IN_DELIVERY -> "배송이 시작되었습니다";
            case CANCELLED -> "주문이 취소되었습니다";
            default -> String.format("주문 상태가 %s에서 %s로 변경되었습니다",
                    getStatusDisplayName(previousStatus),
                    getStatusDisplayName(newStatus));
        };
    }

    /**
     * 상태 변경 정보 생성
     * @param newStatus 새 상태
     * @return 상태 변경 정보
     */
    private static StatusChangeInfo generateStatusChangeInfo(OrderStatus newStatus) {
        return StatusChangeInfo.builder()
                .isDelayed(false)
                .expectedShipDate(null)
                .requiresTracking(newStatus == OrderStatus.READY_FOR_SHIPMENT)
                .nextStepDescription(generateNextStepDescription(newStatus, false))
                .build();
    }

    /**
     * 다음 단계 안내 메시지 생성
     * @param newStatus 새 상태
     * @param isDelayed 지연 여부
     * @return 다음 단계 안내
     */
    private static String generateNextStepDescription(OrderStatus newStatus, boolean isDelayed) {
        if (isDelayed) {
            return "지연 사유가 고객에게 안내되었습니다. 상품 준비 완료 시 상태를 변경해주세요";
        }

        return switch (newStatus) {
            case PREPARING -> "상품을 준비하고 포장을 완료하면 '배송준비완료'로 상태를 변경해주세요";
            case READY_FOR_SHIPMENT -> "택배사에 접수하고 운송장 번호를 등록하면 배송이 시작됩니다";
            case IN_DELIVERY -> "배송이 완료되면 자동으로 '배송완료' 상태로 변경됩니다";
            case CANCELLED -> "취소 처리가 완료되었습니다";
            default -> "처리가 완료되었습니다";
        };
    }

    /**
     * 주문 상태 표시명 반환
     * @param status 주문 상태
     * @return 상태 표시명
     */
    private static String getStatusDisplayName(OrderStatus status) {
        return switch (status) {
            case PAYMENT_PENDING -> "결제대기";
            case PAYMENT_COMPLETED -> "결제완료";
            case PREPARING -> "상품준비중";
            case READY_FOR_SHIPMENT -> "배송준비완료";
            case IN_DELIVERY -> "배송중";
            case DELIVERED -> "배송완료";
            case CANCELLED -> "주문취소";
            case REFUND_PROCESSING -> "환불처리중";
            case REFUNDED -> "환불완료";
        };
    }

    /**
     * 상태 변경이 성공했는지 확인
     * @return 성공 여부
     */
    public boolean isSuccessful() {
        return newStatus != null && !newStatus.equals(previousStatus);
    }

    /**
     * 운송장 등록이 필요한 상태인지 확인
     * @return 운송장 등록 필요 여부
     */
    public boolean requiresTrackingNumber() {
        return statusChangeInfo != null &&
               Boolean.TRUE.equals(statusChangeInfo.requiresTracking());
    }

    /**
     * 출고 지연 상태인지 확인
     * @return 출고 지연 여부
     */
    public boolean isDelayed() {
        return statusChangeInfo != null &&
               Boolean.TRUE.equals(statusChangeInfo.isDelayed());
    }
}