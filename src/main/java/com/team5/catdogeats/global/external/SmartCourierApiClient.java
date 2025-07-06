package com.team5.catdogeats.global.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 스마트택배 API 클라이언트
 * OpenFeign을 사용하여 스마트택배 배송 추적 API와 연동
 * API 제한사항:
 * - 프리티어: 동일 운송장 일 최대 10건 조회 제한
 * - 일일 전체 호출 제한: 1000건
 */
@FeignClient(
        name = "smart-courier-api",
        url = "${smart-courier.api.base-url}",
        configuration = SmartCourierApiConfiguration.class
)
public interface SmartCourierApiClient {

    /**
     * 배송 조회 API
     * 운송장 번호로 배송 상태를 조회합니다.
     *
     * @param t_key API 키
     * @param t_code 택배사 코드 (01: 우체국, 04: CJ대한통운, 05: 한진, 06: 로젠, 08: 롯데)
     * @param t_invoice 운송장 번호
     * @return 배송 추적 정보
     */
    @GetMapping("/trackingInfo")
    SmartCourierTrackingResponse getTrackingInfo(
            @RequestParam("t_key") String t_key,
            @RequestParam("t_code") String t_code,
            @RequestParam("t_invoice") String t_invoice
    );

    /**
     * 택배사 리스트 조회 API
     * 지원하는 택배사 목록을 조회합니다.
     *
     * @param t_key API 키
     * @return 택배사 목록
     */
    @GetMapping("/companylist")
    SmartCourierCompanyListResponse getCompanyList(
            @RequestParam("t_key") String t_key
    );

    /**
     * 운송장 번호 유효성 검증 API
     * 운송장 번호가 올바른 형식인지 검증합니다.
     *
     * @param t_key API 키
     * @param t_code 택배사 코드
     * @param t_invoice 운송장 번호
     * @return 유효성 검증 결과
     */
    @GetMapping("/invoice")
    SmartCourierValidationResponse validateInvoice(
            @RequestParam("t_key") String t_key,
            @RequestParam("t_code") String t_code,
            @RequestParam("t_invoice") String t_invoice
    );

    /**
     * 스마트택배 배송 추적 응답 DTO
     */
    record SmartCourierTrackingResponse(
            String level,           // 배송 상태 레벨 (1: 배송준비중, 2: 집화완료, 3: 배송중, 4: 배송완료)
            String manName,         // 배송담당자
            String manPic,          // 배송담당자 연락처
            String receiverName,    // 수령인
            String receiverAddr,    // 수령 주소
            String itemName,        // 상품명
            String invoiceNo,       // 운송장번호
            String orderNumber,     // 주문번호
            String adUrl,           // 광고 URL
            String estimate,        // 배송 예정일
            String productInfo,     // 상품 정보
            String zipCode,         // 우편번호
            String complete,        // 배송완료여부 ('Y': 완료, 'N': 미완료)
            TrackingDetail[] trackingDetails  // 배송 상세 이력
    ) {

        /**
         * 배송 완료 여부 확인
         * @return 배송 완료 시 true
         */
        public boolean isDelivered() {
            return "Y".equals(complete) || "4".equals(level);
        }

        /**
         * 배송 상태 메시지 반환
         * @return 현재 배송 상태 설명
         */
        public String getStatusMessage() {
            return switch (level) {
                case "1" -> "배송준비중";
                case "2" -> "집화완료";
                case "3" -> "배송중";
                case "4" -> "배송완료";
                default -> "상태불명";
            };
        }
    }

    /**
     * 배송 상세 이력 정보
     */
    record TrackingDetail(
            String timeString,      // 처리일시
            String where,          // 처리장소
            String kind,           // 처리상태
            String telno,          // 연락처
            String telno2,         // 연락처2
            String remark          // 비고
    ) {}

    /**
     * 택배사 목록 응답 DTO
     */
    record SmartCourierCompanyListResponse(
            CompanyInfo[] Company  // 택배사 정보 배열
    ) {}

    /**
     * 택배사 정보
     */
    record CompanyInfo(
            String Code,           // 택배사 코드
            String Name,           // 택배사명
            String International   // 국제배송 지원여부
    ) {}

    /**
     * 운송장 유효성 검증 응답 DTO
     */
    record SmartCourierValidationResponse(
            String status,         // 상태 ('TRUE': 유효, 'FALSE': 무효)
            String message,        // 메시지
            String invoiceNo       // 운송장 번호
    ) {

        /**
         * 유효한 운송장 번호인지 확인
         * @return 유효한 경우 true
         */
        public boolean isValid() {
            return "TRUE".equals(status);
        }
    }

    /**
     * API 오류 응답 처리를 위한 기본 응답 인터페이스
     */
    interface SmartCourierBaseResponse {
        default boolean isSuccess() {
            return true; // 기본적으로 성공으로 간주
        }
    }
}