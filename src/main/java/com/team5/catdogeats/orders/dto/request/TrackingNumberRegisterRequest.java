package com.team5.catdogeats.orders.dto.request;

import com.team5.catdogeats.orders.domain.enums.CourierCompany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 운송장 번호 등록 요청 DTO
 * API: POST /v1/sellers/orders/tracking-number
 *
 * 판매자가 택배사에서 발급받은 운송장 번호를 등록하여 배송을 시작할 때 사용하는 요청 구조
 */
public record TrackingNumberRegisterRequest(

        /**
         * 운송장 번호를 등록할 주문 번호
         */
        @NotBlank(message = "주문 번호는 필수입니다")
        String orderNumber,

        /**
         * 택배사
         * CourierCompany enum 값 중 하나여야 함
         */
        @NotNull(message = "택배사는 필수입니다")
        CourierCompany courierCompany,

        /**
         * 운송장 번호
         * 택배사에서 발급받은 실제 운송장 번호
         * 영문, 숫자, 하이픈만 허용 (택배사별로 형식이 다를 수 있음)
         */
        @NotBlank(message = "운송장 번호는 필수입니다")
        @Size(min = 8, max = 50, message = "운송장 번호는 8~50자 사이여야 합니다")
        @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "운송장 번호는 영문, 숫자, 하이픈만 입력 가능합니다")
        String trackingNumber,

        /**
         * 배송 메모 (선택사항)
         * 배송 시 특별히 주의할 사항이나 배송 관련 메모
         */
        @Size(max = 200, message = "배송 메모는 200자를 초과할 수 없습니다")
        String shipmentMemo,

        /**
         * 즉시 배송 시작 여부
         * true: 운송장 등록과 동시에 배송 중 상태로 변경
         * false: 운송장만 등록하고 별도로 상태 변경 필요
         */
        Boolean startShipmentImmediately,

        /**
         * 스마트택배 API 연동 여부
         * true: 운송장 번호 유효성을 스마트택배 API로 검증
         * false: 유효성 검증 없이 등록 (API 장애 시 사용)
         */
        Boolean enableApiValidation
) {

    /**
     * Compact Constructor - 기본값 설정 및 유효성 검증
     */
    public TrackingNumberRegisterRequest {
        // 기본값 설정
        if (startShipmentImmediately == null) {
            startShipmentImmediately = true;
        }
        if (enableApiValidation == null) {
            enableApiValidation = true;
        }

        // 운송장 번호 기본 형식 검증
        if (trackingNumber != null && courierCompany != null) {
            if (!isValidTrackingNumberFormat()) {
                throw new IllegalArgumentException(
                        String.format("%s의 운송장 번호 형식이 올바르지 않습니다", courierCompany.getDisplayName()));
            }
        }
    }

    /**
     * 택배사의 API 코드 반환
     * @return 스마트택배 API에서 사용하는 택배사 코드
     */
    public String getCourierApiCode() {
        return courierCompany != null ? courierCompany.getApiCode() : null;
    }

    /**
     * 택배사의 표시명 반환
     * @return 사용자에게 표시되는 택배사명
     */
    public String getCourierDisplayName() {
        return courierCompany != null ? courierCompany.getDisplayName() : null;
    }

    /**
     * 정규화된 운송장 번호 반환
     * 공백 제거 및 대문자 변환
     * @return 정규화된 운송장 번호
     */
    public String getNormalizedTrackingNumber() {
        if (trackingNumber == null) {
            return null;
        }
        return trackingNumber.trim().toUpperCase();
    }

    /**
     * 스마트택배 API 검증 수행 여부
     * @return API 검증을 수행할지 여부
     */
    public boolean shouldValidateWithApi() {
        return Boolean.TRUE.equals(enableApiValidation);
    }

    /**
     * 즉시 배송 시작 여부
     * @return 운송장 등록과 동시에 배송 시작할지 여부
     */
    public boolean shouldStartShipmentImmediately() {
        return Boolean.TRUE.equals(startShipmentImmediately);
    }

    /**
     * 운송장 번호 유효성 검증 (기본 형식)
     * 스마트택배 API 호출 전 기본적인 형식 검증
     * @return 유효한 형식인지 여부
     */
    public boolean isValidTrackingNumberFormat() {
        if (trackingNumber == null || trackingNumber.trim().isEmpty() || courierCompany == null) {
            return false;
        }

        return courierCompany.isValidTrackingNumberFormat(getNormalizedTrackingNumber());
    }

    /**
     * 배송 메모 존재 여부
     * @return 배송 메모가 있는지 여부
     */
    public boolean hasShipmentMemo() {
        return shipmentMemo != null && !shipmentMemo.trim().isEmpty();
    }

    /**
     * 배송 추적 URL 생성
     * @return 택배사별 배송 추적 URL
     */
    public String generateTrackingUrl() {
        if (courierCompany == null || trackingNumber == null) {
            return null;
        }
        return courierCompany.generateTrackingUrl(getNormalizedTrackingNumber());
    }

    /**
     * 요청 정보 요약
     * 로깅 및 디버깅용
     * @return 요청 정보 요약 문자열
     */
    public String getSummary() {
        return String.format("주문:%s, 택배사:%s, 운송장:%s, 즉시시작:%s, API검증:%s",
                orderNumber,
                getCourierDisplayName(),
                trackingNumber,
                shouldStartShipmentImmediately(),
                shouldValidateWithApi());
    }

    /**
     * 민감정보 마스킹된 요청 정보
     * 로그에서 운송장 번호를 부분적으로 마스킹
     * @return 마스킹된 요청 정보
     */
    public String getMaskedSummary() {
        String maskedTracking = trackingNumber != null && trackingNumber.length() > 4
                ? trackingNumber.substring(0, 4) + "****" + trackingNumber.substring(trackingNumber.length() - 2)
                : "****";

        return String.format("주문:%s, 택배사:%s, 운송장:%s, 즉시시작:%s, API검증:%s",
                orderNumber,
                getCourierDisplayName(),
                maskedTracking,
                shouldStartShipmentImmediately(),
                shouldValidateWithApi());
    }

    /**
     * 요청 유효성 전체 검증
     * @return 유효한 요청인지 여부
     */
    public boolean isValidRequest() {
        return orderNumber != null && !orderNumber.trim().isEmpty() &&
                courierCompany != null &&
                isValidTrackingNumberFormat();
    }

    /**
     * 정적 팩토리 메서드 - 기본 운송장 등록
     * @param orderNumber 주문 번호
     * @param courierCompany 택배사
     * @param trackingNumber 운송장 번호
     * @return 운송장 등록 요청
     */
    public static TrackingNumberRegisterRequest of(
            String orderNumber,
            CourierCompany courierCompany,
            String trackingNumber) {
        return new TrackingNumberRegisterRequest(
                orderNumber, courierCompany, trackingNumber, null, true, true);
    }

    /**
     * 정적 팩토리 메서드 - 메모 포함 운송장 등록
     * @param orderNumber 주문 번호
     * @param courierCompany 택배사
     * @param trackingNumber 운송장 번호
     * @param shipmentMemo 배송 메모
     * @return 운송장 등록 요청
     */
    public static TrackingNumberRegisterRequest withMemo(
            String orderNumber,
            CourierCompany courierCompany,
            String trackingNumber,
            String shipmentMemo) {
        return new TrackingNumberRegisterRequest(
                orderNumber, courierCompany, trackingNumber, shipmentMemo, true, true);
    }

    /**
     * 정적 팩토리 메서드 - 상태 변경 없이 운송장만 등록
     * @param orderNumber 주문 번호
     * @param courierCompany 택배사
     * @param trackingNumber 운송장 번호
     * @return 운송장 등록 요청 (배송 시작 안함)
     */
    public static TrackingNumberRegisterRequest registerOnly(
            String orderNumber,
            CourierCompany courierCompany,
            String trackingNumber) {
        return new TrackingNumberRegisterRequest(
                orderNumber, courierCompany, trackingNumber, null, false, true);
    }

    /**
     * 정적 팩토리 메서드 - API 검증 없이 운송장 등록
     * @param orderNumber 주문 번호
     * @param courierCompany 택배사
     * @param trackingNumber 운송장 번호
     * @return 운송장 등록 요청 (API 검증 안함)
     */
    public static TrackingNumberRegisterRequest withoutValidation(
            String orderNumber,
            CourierCompany courierCompany,
            String trackingNumber) {
        return new TrackingNumberRegisterRequest(
                orderNumber, courierCompany, trackingNumber, null, true, false);
    }

    /**
     * 정적 팩토리 메서드 - 완전한 운송장 등록 요청
     * @param orderNumber 주문 번호
     * @param courierCompany 택배사
     * @param trackingNumber 운송장 번호
     * @param shipmentMemo 배송 메모
     * @param startShipment 즉시 배송 시작 여부
     * @param enableValidation API 검증 여부
     * @return 운송장 등록 요청
     */
    public static TrackingNumberRegisterRequest complete(
            String orderNumber,
            CourierCompany courierCompany,
            String trackingNumber,
            String shipmentMemo,
            boolean startShipment,
            boolean enableValidation) {
        return new TrackingNumberRegisterRequest(
                orderNumber, courierCompany, trackingNumber, shipmentMemo, startShipment, enableValidation);
    }
    public boolean shouldStartShipment() {
        return startShipment;
    }
}