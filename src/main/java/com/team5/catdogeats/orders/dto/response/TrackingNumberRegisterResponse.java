package com.team5.catdogeats.orders.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team5.catdogeats.orders.domain.enums.CourierCompany;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import lombok.Builder;

import java.time.ZonedDateTime;

/**
 * 운송장 번호 등록 응답 DTO
 * API: POST /v1/sellers/orders/tracking-number
 *
 * 판매자의 운송장 번호 등록 요청 처리 결과를 담는 응답 구조
 */
@Builder
public record TrackingNumberRegisterResponse(
        String orderNumber,
        String trackingNumber,
        CourierCompany courierCompany,
        OrderStatus orderStatus,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        ZonedDateTime shippedAt,

        String message,
        ValidationResult validationResult,
        ShipmentInfo shipmentInfo
) {

    /**
     * 운송장 번호 검증 결과
     */
    @Builder
    public record ValidationResult(
            boolean isValidated,        // API 검증 수행 여부
            boolean isValid,            // 유효한 운송장 번호 여부
            String validationMessage,   // 검증 결과 메시지
            String apiResponseCode,     // 스마트택배 API 응답 코드
            String apiResponseMessage   // 스마트택배 API 응답 메시지
    ) {
        /**
         * 검증 성공 결과 생성
         */
        public static ValidationResult success(String apiResponseMessage) {
            return ValidationResult.builder()
                    .isValidated(true)
                    .isValid(true)
                    .validationMessage("운송장 번호가 유효합니다")
                    .apiResponseCode("200")
                    .apiResponseMessage(apiResponseMessage)
                    .build();
        }

        /**
         * 검증 실패 결과 생성 (유효하지 않은 운송장)
         */
        public static ValidationResult invalid(String errorMessage) {
            return ValidationResult.builder()
                    .isValidated(true)
                    .isValid(false)
                    .validationMessage("운송장 번호가 유효하지 않습니다")
                    .apiResponseCode("400")
                    .apiResponseMessage(errorMessage)
                    .build();
        }

        /**
         * 검증 오류 결과 생성 (API 호출 실패)
         */
        public static ValidationResult error(String errorMessage) {
            return ValidationResult.builder()
                    .isValidated(false)
                    .isValid(false)
                    .validationMessage("검증 중 오류가 발생했습니다")
                    .apiResponseCode("500")
                    .apiResponseMessage(errorMessage)
                    .build();
        }

        /**
         * 검증 생략 결과 생성
         */
        public static ValidationResult skipped(String reason) {
            return ValidationResult.builder()
                    .isValidated(false)
                    .isValid(true) // 검증 생략 시 유효하다고 가정
                    .validationMessage(reason)
                    .apiResponseCode(null)
                    .apiResponseMessage(null)
                    .build();
        }
    }

    /**
     * 배송 정보
     */
    @Builder
    public record ShipmentInfo(
            String recipientName,       // 수령인 이름
            String recipientPhone,      // 수령인 연락처 (마스킹)
            String shippingAddress,     // 배송지 주소
            String deliveryNote,        // 배송 요청사항
            String estimatedDeliveryDate, // 예상 배송 완료일
            String trackingUrl          // 배송 추적 URL
    ) {}

    /**
     * 성공 응답 생성 - 기본
     * @param orderNumber 주문 번호
     * @param trackingNumber 운송장 번호
     * @param courierCompany 택배사
     * @param orderStatus 주문 상태
     * @param shippedAt 발송 시간
     * @return 운송장 등록 응답 DTO
     */
    public static TrackingNumberRegisterResponse success(
            String orderNumber,
            String trackingNumber,
            CourierCompany courierCompany,
            OrderStatus orderStatus,
            ZonedDateTime shippedAt
    ) {
        return TrackingNumberRegisterResponse.builder()
                .orderNumber(orderNumber)
                .trackingNumber(trackingNumber)
                .courierCompany(courierCompany)
                .orderStatus(orderStatus)
                .shippedAt(shippedAt)
                .message("운송장 번호가 성공적으로 등록되었습니다")
                .validationResult(ValidationResult.skipped("검증을 수행하지 않았습니다"))
                .shipmentInfo(null)
                .build();
    }

    /**
     * 성공 응답 생성 - 검증 결과 포함
     * @param orderNumber 주문 번호
     * @param trackingNumber 운송장 번호
     * @param courierCompany 택배사
     * @param orderStatus 주문 상태
     * @param shippedAt 발송 시간
     * @param validationResult 검증 결과
     * @return 운송장 등록 응답 DTO
     */
    public static TrackingNumberRegisterResponse successWithValidation(
            String orderNumber,
            String trackingNumber,
            CourierCompany courierCompany,
            OrderStatus orderStatus,
            ZonedDateTime shippedAt,
            ValidationResult validationResult
    ) {
        String message = validationResult.isValid()
                ? "운송장 번호가 검증되어 성공적으로 등록되었습니다"
                : "운송장 번호가 등록되었으나 검증에 실패했습니다";

        return TrackingNumberRegisterResponse.builder()
                .orderNumber(orderNumber)
                .trackingNumber(trackingNumber)
                .courierCompany(courierCompany)
                .orderStatus(orderStatus)
                .shippedAt(shippedAt)
                .message(message)
                .validationResult(validationResult)
                .shipmentInfo(null)
                .build();
    }

    /**
     * 성공 응답 생성 - 전체 정보 포함
     * @param orderNumber 주문 번호
     * @param trackingNumber 운송장 번호
     * @param courierCompany 택배사
     * @param orderStatus 주문 상태
     * @param shippedAt 발송 시간
     * @param validationResult 검증 결과
     * @param shipmentInfo 배송 정보
     * @return 운송장 등록 응답 DTO
     */
    public static TrackingNumberRegisterResponse fullResponse(
            String orderNumber,
            String trackingNumber,
            CourierCompany courierCompany,
            OrderStatus orderStatus,
            ZonedDateTime shippedAt,
            ValidationResult validationResult,
            ShipmentInfo shipmentInfo
    ) {
        String message = validationResult.isValid()
                ? "운송장 번호가 검증되어 배송이 시작되었습니다"
                : "운송장 번호가 등록되었으나 검증에 실패했습니다";

        return TrackingNumberRegisterResponse.builder()
                .orderNumber(orderNumber)
                .trackingNumber(trackingNumber)
                .courierCompany(courierCompany)
                .orderStatus(orderStatus)
                .shippedAt(shippedAt)
                .message(message)
                .validationResult(validationResult)
                .shipmentInfo(shipmentInfo)
                .build();
    }

    /**
     * 배송 추적 URL 생성
     * @return 택배사별 배송 추적 URL
     */
    public String generateTrackingUrl() {
        if (courierCompany == null || trackingNumber == null) {
            return null;
        }

        return switch (courierCompany) {
            case POST_OFFICE -> "https://service.epost.go.kr/trace.RetrieveTrace.comm?searchKey=" + trackingNumber;
            case CJ_LOGISTICS -> "https://www.cjlogistics.com/ko/tool/parcel/tracking?track=" + trackingNumber;
            case HANJIN -> "https://www.hanjin.co.kr/kor/CMS/DeliveryMgr/WaybillResult.do?mCode=MN038&no=" + trackingNumber;
            case LOGEN -> "https://www.ilogen.com/web/personal/trace/" + trackingNumber;
            case LOTTE -> "https://www.lotteglogis.com/home/reservation/tracking/linkView?invoice_no=" + trackingNumber;
        };
    }
}