package com.team5.catdogeats.orders.service;

/**
 * 배송 추적 서비스 인터페이스
 * 스마트택배 배송 추적 API를 호출하여 배송 상태를 확인하는 서비스
 *
 * API 제한사항 관리:
 * - 프리티어: 동일 운송장 일 최대 10건 조회 제한
 * - 일일 전체 호출 제한: 1000건
 * - 8시간마다 배치 실행으로 제한 준수
 */
public interface DeliveryTrackingService {

    /**
     * 배송 상태 확인
     * 운송장 번호와 택배사 코드로 배송 완료 여부를 확인합니다.
     *
     * @param courierCode 택배사 코드 (01, 04, 05, 06, 08)
     * @param trackingNumber 운송장 번호
     * @return 배송 완료 여부 (true: 배송 완료, false: 배송 중)
     * @throws DeliveryTrackingApiException API 호출 실패 시
     */
    boolean checkDeliveryStatus(String courierCode, String trackingNumber);

    /**
     * 운송장 번호 유효성 검증
     * 스마트택배 API를 통해 운송장 번호가 유효한지 확인합니다.
     *
     * @param courierCode 택배사 코드
     * @param trackingNumber 운송장 번호
     * @return 유효성 검증 결과
     */
    ValidationResult validateTrackingNumber(String courierCode, String trackingNumber);

    /**
     * API 호출 현황 조회
     * 일일 호출 현황과 제한 상태를 확인합니다.
     *
     * @return API 호출 현황 정보
     */
    ApiCallStatus getApiCallStatus();

    /**
     * 유효성 검증 결과
     */
    record ValidationResult(
            boolean isValidated,    // API 검증 수행 여부
            boolean isValid,        // 유효한 운송장 번호 여부
            String message,         // 검증 결과 메시지
            String type             // 결과 타입 (SUCCESS, INVALID, ERROR, SKIPPED)
    ) {
        public static ValidationResult success(String message) {
            return new ValidationResult(true, true, message, "SUCCESS");
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(true, false, message, "INVALID");
        }

        public static ValidationResult error(String message) {
            return new ValidationResult(false, false, message, "ERROR");
        }

        public static ValidationResult skipped(String message) {
            return new ValidationResult(false, false, message, "SKIPPED");
        }
    }

    /**
     * API 호출 현황
     */
    record ApiCallStatus(
            int dailyCallCount,         // 오늘 총 호출 수
            int dailyTotalLimit,        // 일일 전체 호출 제한
            int trackingNumbersCount,   // 오늘 조회한 운송장 개수
            String lastResetDate        // 마지막 초기화 날짜
    ) {
        public boolean isNearLimit() {
            return dailyCallCount > (dailyTotalLimit * 0.8);
        }

        public int getRemainingCalls() {
            return Math.max(0, dailyTotalLimit - dailyCallCount);
        }
    }

    /**
     * 배송 추적 API 예외
     */
    class DeliveryTrackingApiException extends RuntimeException {
        public DeliveryTrackingApiException(String message) {
            super(message);
        }

        public DeliveryTrackingApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}