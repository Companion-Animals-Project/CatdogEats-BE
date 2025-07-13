package com.team5.catdogeats.users.domain.dto;

import com.team5.catdogeats.addresses.dto.AddressResponseDto;
import com.team5.catdogeats.orders.domain.dto.SellerStoreStatsDTO;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 판매자 스토어 페이지 정보 DTO
 * Orders 도메인의 SellerStoreStatsDTO 사용
 */
@Schema(description = "판매자 스토어 정보")
public record SellerStoreInfoDTO(
        @Schema(description = "판매자 ID", example = "2ceb807f-586f-4450-b470-d1ece7173749")
        String sellerId,

        @Schema(description = "상점명", example = "멍멍이네 수제간식")
        String vendorName,

        @Schema(description = "상점 프로필 이미지", example = "https://example.com/profile.jpg")
        String vendorProfileImage,

        @Schema(description = "태그", example = "수제간식,강아지")
        String tags,

        @Schema(description = "운영 시작시간", example = "09:00")
        String operatingStartTime,

        @Schema(description = "운영 종료시간", example = "18:00")
        String operatingEndTime,

        @Schema(description = "휴무일", example = "월요일,화요일")
        String closedDays,

        @Schema(description = "사업자 주소 정보")
        StoreAddressInfo storeAddress,

        @Schema(description = "운영 시작년도", example = "2020")
        String operationStartYear,

        @Schema(description = "총 상품 수", example = "50")
        Long totalProducts,

        @Schema(description = "전체 상품 판매량", example = "1250")
        Long totalSalesQuantity,

        @Schema(description = "평균 배송 소요일", example = "3.2")
        Double avgDeliveryDays,

        @Schema(description = "총 리뷰 수", example = "85")
        Long totalReviews

) {

    /**
     * 스토어 주소 정보를 위한 중첩 레코드
     */
    @Schema(description = "스토어 주소 정보")
    public record StoreAddressInfo(
            @Schema(description = "주소 제목", example = "본사")
            String title,

            @Schema(description = "전체 주소", example = "서울시 강남구 역삼동 테헤란로 123 101호")
            String fullAddress,

            @Schema(description = "우편번호", example = "12345")
            String postalCode,

            @Schema(description = "전화번호", example = "02-1234-5678")
            String phoneNumber
    ) {
        public static StoreAddressInfo from(AddressResponseDto address) {
            if (address == null) {
                return null;
            }
            return new StoreAddressInfo(
                    address.getTitle(),
                    address.getFullAddress(),
                    address.getPostalCode(),
                    address.getPhoneNumber()
            );
        }
    }

    /**
     * Orders 도메인의 SellerStoreStats와 주소 정보를 사용하여 생성
     */
    public static SellerStoreInfoDTO from(Sellers seller, Long totalProducts, SellerStoreStatsDTO stats, AddressResponseDto businessAddress) {
        if (seller == null) {
            return null;
        }

        String operatingStartTime = formatTimeOnly(seller.getOperatingStartTime());
        String operatingEndTime = formatTimeOnly(seller.getOperatingEndTime());
        String operationStartYear = extractYear(seller.getCreatedAt());

        // Orders 도메인의 SellerStoreStatsDTO 사용
        Long totalSalesCount = stats != null ? stats.totalSalesCount() : 0L;
        Double avgDeliveryDays = stats != null ? stats.avgDeliveryDays() : 0.0;
        Long totalReviews = stats != null ? stats.totalReviews() : 0L;

        return new SellerStoreInfoDTO(
                seller.getUserId(),
                seller.getVendorName(),
                seller.getVendorProfileImage(),
                seller.getTags(),
                operatingStartTime,
                operatingEndTime,
                seller.getClosedDays(), // 휴무일 추가
                StoreAddressInfo.from(businessAddress), // 주소 정보 추가
                operationStartYear,
                totalProducts,
                totalSalesCount,
                avgDeliveryDays,
                totalReviews
        );
    }

    /**
     * 기존 호환성을 위한 메서드 (주소 정보 없이)
     */
    public static SellerStoreInfoDTO from(Sellers seller, Long totalProducts, SellerStoreStatsDTO stats) {
        return from(seller, totalProducts, stats, null);
    }

    /**
     *  LocalTime -> "HH:mm" 문자열 변환
     */
    private static String formatTimeOnly(LocalTime time) {
        if (time == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }

    /**
     *  ZoneDateTime -> 연도만 문자열로 추출
     */
    private static String extractYear(java.time.ZonedDateTime createdAt) {
        if (createdAt == null) {
            return null;
        }
        return String.valueOf(createdAt.getYear());
    }
}