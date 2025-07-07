package com.team5.catdogeats.orders.dto.request;

import com.team5.catdogeats.orders.domain.enums.CourierCompany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 운송장 번호 등록 요청 DTO
 * API: POST /v1/sellers/orders/tracking-number
 * 판매자가 택배사에서 발급받은 운송장 번호를 등록하여 배송을 시작할 때 사용하는 요청 구조
 */
public record TrackingNumberRegisterRequest(

        /*
         * 운송장 번호를 등록할 주문 번호
         */
        @NotBlank(message = "주문 번호는 필수입니다")
        String orderNumber,

        /*
         * 택배사
         * CourierCompany enum 값 중 하나여야 함
         */
        @NotNull(message = "택배사는 필수입니다")
        CourierCompany courierCompany,

        /*
         * 운송장 번호
         * 택배사에서 발급받은 실제 운송장 번호
         * 영문, 숫자, 하이픈만 허용 (택배사별로 형식이 다를 수 있음)
         */
        @NotBlank(message = "운송장 번호는 필수입니다")
        @Size(min = 8, max = 50, message = "운송장 번호는 8~50자 사이여야 합니다")
        @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "운송장 번호는 영문, 숫자, 하이픈만 입력 가능합니다")
        String trackingNumber,

        /*
         * 배송 메모 (선택사항)
         * 배송 시 특별히 주의할 사항이나 배송 관련 메모
         */
        @Size(max = 200, message = "배송 메모는 200자를 초과할 수 없습니다")
        String shipmentMemo,

        /*
         * 즉시 배송 시작 여부
         * true: 운송장 등록과 동시에 배송 중 상태로 변경
         * false: 운송장만 등록하고 별도로 상태 변경 필요
         */
        Boolean startShipmentImmediately,

        /*
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

    // ===== 접근자 메서드들 (일관성 확보) =====

    /**
     * 즉시 배송 시작 여부 반환 (주 접근자)
     */
    public Boolean shouldStartShipmentImmediately() {
        return startShipmentImmediately;
    }

    /**
     * 간단한 배송 시작 여부 체크 (호환성 유지)
     */
    public boolean shouldStartShipment() {
        return Boolean.TRUE.equals(startShipmentImmediately);
    }

    /**
     * API 검증 여부 반환
     */
    public Boolean shouldValidateWithApi() {
        return enableApiValidation;
    }

    /**
     * API 검증 여부 반환 (기본 접근자)
     */
    public Boolean enableApiValidation() {
        return enableApiValidation;
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
        return courierCompany != null ? courierCompany.getDisplayName() : "알 수 없음";
    }

    /**
     * 운송장 번호 형식 검증
     * @return 유효한 형식인지 여부
     */
    private boolean isValidTrackingNumberFormat() {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return false;
        }

        // 기본 길이 검증
        if (trackingNumber.length() < 8 || trackingNumber.length() > 50) {
            return false;
        }

        // 택배사별 세부 검증 (간소화)
        return trackingNumber.matches("^[A-Za-z0-9\\-]+$");
    }

    /**
     * 디버깅용 정보 출력
     * @return 디버깅 정보 문자열
     */
    @Override
    public String toString() {
        // 운송장 번호 마스킹 처리
        String maskedTracking = trackingNumber != null && trackingNumber.length() > 6
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

    // ===== 정적 팩토리 메서드들 =====

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
}