package com.team5.catdogeats.orders.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * 주문 상태 변경 응답 DTO
 * API: POST /v1/sellers/orders/status
 *
 * 판매자의 주문 상태 변경 요청 처리 결과를 담는 응답 구조
 */
@Builder
public record OrderStatusUpdateResponse(
        String orderNumber,
        OrderStatus previousStatus,
        OrderStatus currentStatus,
        String statusChangeReason,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        ZonedDateTime updatedAt,

        String message,
        AdditionalInfo additionalInfo
) {

    /**
     * 추가 정보 (상황에 따라 포함)
     */
    @Builder
    public record AdditionalInfo(
            String expectedShipDate,    // 예상 출고일 (지연 시)
            String trackingNumber,      // 운송장 번호 (배송 시작 시)
            String courierCompany,      // 택배사 (배송 시작 시)
            String nextAction           // 다음 필요한 액션 안내
    ) {}

    /**
     * 성공 응답 생성 - 기본 정보만
     * @param orderNumber 주문 번호
     * @param previousStatus 이전 상태
     * @param currentStatus 변경된 상태
     * @param reason 변경 사유
     * @param updatedAt 변경 시간
     * @return 상태 변경 응답 DTO
     */
    public static OrderStatusUpdateResponse success(
            String orderNumber,
            OrderStatus previousStatus,
            OrderStatus currentStatus,
            String reason,
            ZonedDateTime updatedAt
    ) {
        String message = generateStatusChangeMessage(previousStatus, currentStatus);

        return OrderStatusUpdateResponse.builder()
                .orderNumber(orderNumber)
                .previousStatus(previousStatus)
                .currentStatus(currentStatus)
                .statusChangeReason(reason)
                .updatedAt(updatedAt)
                .message(message)
                .additionalInfo(null)
                .build();
    }

    /**
     * 성공 응답 생성 - 추가 정보 포함
     * @param orderNumber 주문 번호
     * @param previousStatus 이전 상태
     * @param currentStatus 변경된 상태
     * @param reason 변경 사유
     * @param updatedAt 변경 시간
     * @param additionalInfo 추가 정보
     * @return 상태 변경 응답 DTO
     */
    public static OrderStatusUpdateResponse successWithAdditionalInfo(
            String orderNumber,
            OrderStatus previousStatus,
            OrderStatus currentStatus,
            String reason,
            ZonedDateTime updatedAt,
            AdditionalInfo additionalInfo
    ) {
        String message = generateStatusChangeMessage(previousStatus, currentStatus);

        return OrderStatusUpdateResponse.builder()
                .orderNumber(orderNumber)
                .previousStatus(previousStatus)
                .currentStatus(currentStatus)
                .statusChangeReason(reason)
                .updatedAt(updatedAt)
                .message(message)
                .additionalInfo(additionalInfo)
                .build();
    }

    /**
     * 출고 지연 응답 생성
     * @param orderNumber 주문 번호
     * @param reason 지연 사유
     * @param expectedShipDate 예상 출고일
     * @param updatedAt 변경 시간
     * @return 출고 지연 응답 DTO
     */
    public static OrderStatusUpdateResponse delayResponse(
            String orderNumber,
            String reason,
            String expectedShipDate,
            ZonedDateTime updatedAt
    ) {
        AdditionalInfo additionalInfo = AdditionalInfo.builder()
                .expectedShipDate(expectedShipDate)
                .nextAction("예상 출고일에 맞춰 상품 준비를 완료해 주세요")
                .build();

        return OrderStatusUpdateResponse.builder()
                .orderNumber(orderNumber)
                .previousStatus(OrderStatus.PREPARING)
                .currentStatus(OrderStatus.PREPARING) // 지연은 상태 변경이 아님
                .statusChangeReason(reason)
                .updatedAt(updatedAt)
                .message("출고 지연이 등록되었습니다")
                .additionalInfo(additionalInfo)
                .build();
    }

    /**
     * 배송 시작 응답 생성
     * @param orderNumber 주문 번호
     * @param trackingNumber 운송장 번호
     * @param courierCompany 택배사
     * @param updatedAt 변경 시간
     * @return 배송 시작 응답 DTO
     */
    public static OrderStatusUpdateResponse shipmentStartResponse(
            String orderNumber,
            String trackingNumber,
            String courierCompany,
            ZonedDateTime updatedAt
    ) {
        AdditionalInfo additionalInfo = AdditionalInfo.builder()
                .trackingNumber(trackingNumber)
                .courierCompany(courierCompany)
                .nextAction("배송 추적은 자동으로 업데이트됩니다")
                .build();

        return OrderStatusUpdateResponse.builder()
                .orderNumber(orderNumber)
                .previousStatus(OrderStatus.READY_FOR_SHIPMENT)
                .currentStatus(OrderStatus.IN_DELIVERY)
                .statusChangeReason("운송장 등록 완료")
                .updatedAt(updatedAt)
                .message("배송이 시작되었습니다")
                .additionalInfo(additionalInfo)
                .build();
    }

    /**
     * 상태 변경에 따른 메시지 생성
     */
    private static String generateStatusChangeMessage(OrderStatus previousStatus, OrderStatus currentStatus) {
        return switch (currentStatus) {
            case PREPARING -> "상품 준비가 시작되었습니다";
            case READY_FOR_SHIPMENT -> "배송 준비가 완료되었습니다";
            case IN_DELIVERY -> "배송이 시작되었습니다";
            case DELIVERED -> "배송이 완료되었습니다";
            case CANCELLED -> "주문이 취소되었습니다";
            default -> "주문 상태가 변경되었습니다";
        };
    }
}