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
        Boolean startShipmentImmediately
) {

    /**
     * Compact Constructor - 기본값 설정 및 유효성 검증
     */
    public TrackingNumberRegisterRequest {
        // 기본값 설정
        if (startShipmentImmediately == null) {
            startShipmentImmediately = true;
        }

        // 운송장 번호 기본 형식 검증
        if (trackingNumber != null && courierCompany != null) {
            if (!isValidTrackingNumberFormat()) {
                throw new IllegalArgumentException(
                        String.format("%s의 운송장 번호 형식이 올바르지 않습니다", courierCompany.getDisplayName()));
            }
        }
    }

    // ===== 접근자 메서드들 =====

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

        // 택배사별 세부 검증
        return courierCompany.isValidTrackingNumberFormat(trackingNumber);
    }

    /**
     * 디버깅용 정보 출력
     * @return 디버깅 정보 문자열
     */
    @Override
    public String toString() {
        // 운송장 번호 마스킹 처리
        String maskedTracking = trackingNumber != null && trackingNumber.length() > 6
                ? trackingNumber.substring(0, 3) + "***" + trackingNumber.substring(trackingNumber.length() - 3)
                : "***";

        return String.format("TrackingNumberRegisterRequest{orderNumber='%s', courierCompany=%s, trackingNumber='%s', shipmentMemo='%s', startShipmentImmediately=%s}",
                orderNumber, courierCompany, maskedTracking, shipmentMemo, startShipmentImmediately);
    }
}