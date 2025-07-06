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
     * 운송장 번호 정규화 (공백 제거, 대문자 변환)
     * @return 정규화된 운송장 번호
     */
    public String getNormalizedTrackingNumber() {
        if (trackingNumber == null) {
            return null;
        }
        return trackingNumber.trim().toUpperCase();
    }

    /**
     * API 검증이 활성화되어 있는지 확인
     * @return API 검증 활성화 여부
     */
    public boolean shouldValidateWithApi() {
        return Boolean.TRUE.equals(enableApiValidation);
    }

    /**
     * 즉시 배송 시작이 요청되었는지 확인
     * @return 즉시 배송 시작 여부
     */
    public boolean shouldStartShipmentImmediately() {
        return Boolean.TRUE.equals(startShipmentImmediately);
    }

    /**
     * 요청 데이터 유효성 검증
     * @return 유효한 요청 데이터 여부
     */
    public boolean isValidRequest() {
        return orderNumber != null && !orderNumber.trim().isEmpty()
                && courierCompany != null
                && trackingNumber != null && !trackingNumber.trim().isEmpty();
    }
}