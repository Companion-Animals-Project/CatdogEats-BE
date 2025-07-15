package com.team5.catdogeats.orders.util;

/**
 * 배송 주소 처리 유틸리티 클래스
 * Address 도메인과 분리하여 Shipments 전용 주소 처리 로직 제공
 */
public class ShippingAddressUtils {

    private ShippingAddressUtils() {
        // 유틸리티 클래스는 인스턴스화 방지
        throw new IllegalStateException("유틸리티 클래스는 인스턴스화할 수 없습니다");
    }

    /**
     * 전체 주소 생성 (도로명 주소 + 상세 주소)
     * Shipments 엔티티의 streetAddress와 detailAddress를 조합
     *
     * @param streetAddress 도로명 주소
     * @param detailAddress 상세 주소
     * @return 조합된 전체 주소
     */
    public static String buildFullAddress(String streetAddress, String detailAddress) {
        if (streetAddress == null && detailAddress == null) {
            return "";
        }

        StringBuilder fullAddress = new StringBuilder();

        if (streetAddress != null && !streetAddress.trim().isEmpty()) {
            fullAddress.append(streetAddress.trim());
        }

        if (detailAddress != null && !detailAddress.trim().isEmpty()) {
            if (!fullAddress.isEmpty()) {
                fullAddress.append(" ");
            }
            fullAddress.append(detailAddress.trim());
        }

        return fullAddress.toString();
    }

    /**
     * 배송지 간략 표시 (목록용)
     * 긴 주소를 지정된 길이로 자르고 말줄임표 추가
     *
     * @param streetAddress 도로명 주소
     * @param detailAddress 상세 주소
     * @param maxLength 최대 표시 길이
     * @return 간략하게 처리된 주소
     */
    public static String buildBriefAddress(String streetAddress, String detailAddress, int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("최대 길이는 0보다 커야 합니다");
        }

        String fullAddress = buildFullAddress(streetAddress, detailAddress);

        if (fullAddress.length() <= maxLength) {
            return fullAddress;
        }

        // 말줄임표를 고려해서 자르기
        return fullAddress.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}