package com.team5.catdogeats.orders.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 스마트택배 운송장 번호 유효성 검증 API 응답 DTO
 * 운송장 번호 형식 및 존재 여부 검증 시 사용
 */
public record TrackingValidationResponse(
        @JsonProperty("code") String code,                    // 응답 코드 ("100": 성공, "200": 실패)
        @JsonProperty("message") String message,              // 응답 메시지
        @JsonProperty("level") String level,                  // 배송 단계
        @JsonProperty("invoiceNo") String invoiceNo,          // 검증한 운송장 번호
        @JsonProperty("itemName") String itemName,            // 상품명 (존재하는 경우)
        @JsonProperty("adUrl") String adUrl                   // 광고 URL (무시)
) {

    /**
     * API 호출 성공 여부 확인
     * @return 성공 시 true
     */
    public boolean isSuccess() {
        return "100".equals(code);
    }

    /**
     * 운송장 번호 유효성 확인
     * API가 성공하고 level이 존재하면 유효한 운송장으로 판단
     * @return 유효한 운송장이면 true
     */
    public boolean isValidTrackingNumber() {
        return isSuccess() && level != null && !level.trim().isEmpty();
    }

    /**
     * 검증 결과 메시지 생성
     * @return 사용자에게 표시할 메시지
     */
    public String getValidationMessage() {
        if (isValidTrackingNumber()) {
            return "유효한 운송장 번호입니다";
        } else if (isSuccess()) {
            return "운송장 번호를 찾을 수 없습니다";
        } else {
            return message != null ? message : "운송장 번호 검증에 실패했습니다";
        }
    }

    /**
     * 성공 응답 생성 (테스트용)
     * @param invoiceNo 운송장 번호
     * @param isValid 유효한 운송장인지 여부
     * @return 테스트용 응답 객체
     */
    public static TrackingValidationResponse mockSuccess(String invoiceNo, boolean isValid) {
        return new TrackingValidationResponse(
                "100",
                "성공",
                isValid ? "1" : null,
                invoiceNo,
                isValid ? "배송 상품" : null,
                null
        );
    }

    /**
     * 실패 응답 생성 (테스트용)
     * @param invoiceNo 운송장 번호
     * @param message 오류 메시지
     * @return 테스트용 실패 응답 객체
     */
    public static TrackingValidationResponse mockError(String invoiceNo, String message) {
        return new TrackingValidationResponse(
                "200",
                message,
                null,
                invoiceNo,
                null,
                null
        );
    }

    /**
     * 잘못된 운송장 번호 응답 생성 (테스트용)
     * @param invoiceNo 운송장 번호
     * @return 테스트용 잘못된 운송장 응답 객체
     */
    public static TrackingValidationResponse mockInvalidTracking(String invoiceNo) {
        return new TrackingValidationResponse(
                "100",
                "성공",
                null, // level이 null이면 존재하지 않는 운송장으로 판단
                invoiceNo,
                null,
                null
        );
    }
}