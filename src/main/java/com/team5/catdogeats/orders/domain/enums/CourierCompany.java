package com.team5.catdogeats.orders.domain.enums;

/**
 * 택배사 열거형
 * 스마트택배 API 연동을 위한 택배사 정보
 * 지원 택배사: 우체국택배, CJ대한통운, 한진택배, 로젠택배, 롯데택배
 */
public enum CourierCompany {

    /**
     * 우체국택배
     * API 코드: 01
     */
    POST_OFFICE("01", "우체국택배"),

    /**
     * CJ대한통운
     * API 코드: 04
     */
    CJ_LOGISTICS("04", "CJ대한통운"),

    /**
     * 한진택배
     * API 코드: 05
     */
    HANJIN("05", "한진택배"),

    /**
     * 로젠택배
     * API 코드: 06
     */
    LOGEN("06", "로젠택배"),

    /**
     * 롯데택배
     * API 코드: 08
     */
    LOTTE("08", "롯데택배");

    private final String apiCode;
    private final String displayName;

    CourierCompany(String apiCode, String displayName) {
        this.apiCode = apiCode;
        this.displayName = displayName;
    }

    /**
     * 스마트택배 API에서 사용하는 택배사 코드
     * @return API 코드
     */
    public String getApiCode() {
        return apiCode;
    }

    /**
     * 사용자에게 표시되는 택배사명
     * @return 택배사 표시명
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * API 코드로 택배사 조회
     * @param apiCode 스마트택배 API 코드
     * @return 해당하는 택배사 (없으면 null)
     */
    public static CourierCompany fromApiCode(String apiCode) {
        if (apiCode == null) {
            return null;
        }

        for (CourierCompany courier : values()) {
            if (courier.apiCode.equals(apiCode)) {
                return courier;
            }
        }
        return null;
    }

    /**
     * 표시명으로 택배사 조회
     * @param displayName 택배사 표시명
     * @return 해당하는 택배사 (없으면 null)
     */
    public static CourierCompany fromDisplayName(String displayName) {
        if (displayName == null) {
            return null;
        }

        for (CourierCompany courier : values()) {
            if (courier.displayName.equals(displayName)) {
                return courier;
            }
        }
        return null;
    }

    /**
     * 운송장 번호 형식 검증
     * 택배사별 기본적인 운송장 번호 형식 확인
     * @param trackingNumber 검증할 운송장 번호
     * @return 유효한 형식인지 여부
     */
    public boolean isValidTrackingNumberFormat(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return false;
        }

        String normalized = trackingNumber.trim().toUpperCase();

        return switch (this) {
            case POST_OFFICE -> normalized.matches("^[0-9]{13}$"); // 13자리 숫자
            case CJ_LOGISTICS -> normalized.matches("^[0-9]{10,12}$"); // 10-12자리 숫자
            case HANJIN -> normalized.matches("^[0-9]{10,12}$"); // 10-12자리 숫자
            case LOGEN -> normalized.matches("^[0-9]{11,12}$"); // 11-12자리 숫자
            case LOTTE -> normalized.matches("^[0-9]{12,13}$"); // 12-13자리 숫자
        };
    }

    /**
     * 배송 추적 URL 생성
     * @param trackingNumber 운송장 번호
     * @return 택배사별 배송 추적 URL
     */
    public String generateTrackingUrl(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return null;
        }

        String normalized = trackingNumber.trim();

        return switch (this) {
            case POST_OFFICE -> "https://service.epost.go.kr/trace.RetrieveTrace.comm?searchKey=" + normalized;
            case CJ_LOGISTICS -> "https://www.cjlogistics.com/ko/tool/parcel/tracking?track=" + normalized;
            case HANJIN -> "https://www.hanjin.co.kr/kor/CMS/DeliveryMgr/WaybillResult.do?mCode=MN038&no=" + normalized;
            case LOGEN -> "https://www.ilogen.com/web/personal/trace/" + normalized;
            case LOTTE -> "https://www.lotteglogis.com/home/reservation/tracking/linkView?invoice_no=" + normalized;
        };
    }
}