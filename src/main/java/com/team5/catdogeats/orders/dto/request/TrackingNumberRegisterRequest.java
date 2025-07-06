package com.team5.catdogeats.orders.dto.request;

import com.team5.catdogeats.orders.domain.enums.CourierCompany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 운송장 번호 등록 요청 DTO
 * API: POST /v1/sellers/orders/tracking-number
 *
 * 판매자가 택배사에서 발급받은 운송장 번호를 등록하여 배송을 시작할 때 사용하는 요청 구조
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class TrackingNumberRegisterRequest {

    /**
     * 운송장 번호를 등록할 주문 번호
     */
    @NotBlank(message = "주문 번호는 필수입니다")
    private String orderNumber;

    /**
     * 택배사
     * CourierCompany enum 값 중 하나여야 함
     */
    @NotNull(message = "택배사는 필수입니다")
    private CourierCompany courierCompany;

    /**
     * 운송장 번호
     * 택배사에서 발급받은 실제 운송장 번호
     * 영문, 숫자, 하이픈만 허용 (택배사별로 형식이 다를 수 있음)
     */
    @NotBlank(message = "운송장 번호는 필수입니다")
    @Size(min = 8, max = 50, message = "운송장 번호는 8~50자 사이여야 합니다")
    @Pattern(regexp = "^[A-Za-z0-9\\-]+$", message = "운송장 번호는 영문, 숫자, 하이픈만 입력 가능합니다")
    private String trackingNumber;

    /**
     * 배송 메모 (선택사항)
     * 배송 시 특별히 주의할 사항이나 배송 관련 메모
     */
    @Size(max = 200, message = "배송 메모는 200자를 초과할 수 없습니다")
    private String shipmentMemo;

    /**
     * 즉시 배송 시작 여부
     * true: 운송장 등록과 동시에 배송 중 상태로 변경
     * false: 운송장만 등록하고 별도로 상태 변경 필요
     */
    @Builder.Default
    private Boolean startShipmentImmediately = true;

    /**
     * 스마트택배 API 연동 여부
     * true: 운송장 번호 유효성을 스마트택배 API로 검증
     * false: 유효성 검증 없이 등록 (API 장애 시 사용)
     */
    @Builder.Default
    private Boolean enableApiValidation = true;

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
        return enableApiValidation != null && enableApiValidation;
    }

    /**
     * 즉시 배송 시작 여부
     * @return 운송장 등록과 동시에 배송 시작할지 여부
     */
    public boolean shouldStartShipmentImmediately() {
        return startShipmentImmediately != null && startShipmentImmediately;
    }

    /**
     * 운송장 번호 유효성 검증 (기본 형식)
     * 스마트택배 API 호출 전 기본적인 형식 검증
     * @return 유효한 형식인지 여부
     */
    public boolean isValidTrackingNumberFormat() {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return false;
        }

        String normalized = getNormalizedTrackingNumber();

        // 기본 길이 체크
        if (normalized.length() < 8 || normalized.length() > 50) {
            return false;
        }

        // 택배사별 형식 검증 (간단한 규칙만)
        return switch (courierCompany) {
            case POST_OFFICE -> normalized.matches("^[0-9]{13}$"); // 13자리 숫자
            case CJ_LOGISTICS -> normalized.matches("^[0-9]{10,12}$"); // 10-12자리 숫자
            case HANJIN -> normalized.matches("^[0-9]{10,12}$"); // 10-12자리 숫자
            case LOGEN -> normalized.matches("^[0-9]{11,12}$"); // 11-12자리 숫자
            case LOTTE -> normalized.matches("^[0-9]{12,13}$"); // 12-13자리 숫자
            default -> true; // 기본적으로 허용
        };
    }

    /**
     * 배송 메모 존재 여부
     * @return 배송 메모가 있는지 여부
     */
    public boolean hasShipmentMemo() {
        return shipmentMemo != null && !shipmentMemo.trim().isEmpty();
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
}