package com.team5.catdogeats.users.domain.dto;

import com.team5.catdogeats.addresses.dto.AddressResponseDto;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.time.ZonedDateTime;

@Schema(description = "판매자 정보 응답 DTO")
public record SellerInfoResponseDTO(

        @Schema(description = "사용자 ID", example = "2ceb807f-586f-4450-b470-d1ece7173749")
        String userId,

        @Schema(description = "업체명", example = "멍멍이네 수제간식")
        String vendorName,

        @Schema(description = "업체 프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String vendorProfileImage,

        @Schema(description = "사업자 등록번호", example = "123-45-67890")
        String businessNumber,

        @Schema(description = "정산 은행명", example = "국민은행")
        String settlementBank,

        @Schema(description = "정산 계좌번호", example = "123456789012")
        String settlementAcc,

        @Schema(description = "태그", example = "수제간식,강아지")
        String tags,

        @Schema(description = "운영 시작 시간", example = "09:00:00")
        LocalTime operatingStartTime,

        @Schema(description = "운영 종료 시간", example = "18:00:00")
        LocalTime operatingEndTime,

        @Schema(description = "휴무일", example = "월요일,화요일")
        String closedDays,

        // 배송비 관련 필드 추가
        @Schema(description = "기본 배송비", example = "3000")
        Long deliveryFee,

        @Schema(description = "무료배송 최소 주문금액", example = "50000")
        Long freeShippingThreshold,

        @Schema(description = "사업자 주소 정보")
        BusinessAddressInfo businessAddress,

        @Schema(description = "생성일시", example = "2024-01-15T10:30:00")
        ZonedDateTime createdAt,

        @Schema(description = "수정일시", example = "2024-01-20T14:20:00")
        ZonedDateTime updatedAt

) {

    @Schema(description = "사업자 주소 정보")
    public record BusinessAddressInfo(
            @Schema(description = "주소 ID", example = "address-uuid")
            String addressId,

            @Schema(description = "주소 제목", example = "본사")
            String title,

            @Schema(description = "시/도", example = "서울시")
            String city,

            @Schema(description = "시/군/구", example = "강남구")
            String district,

            @Schema(description = "읍/면/동", example = "역삼동")
            String neighborhood,

            @Schema(description = "도로명 주소", example = "테헤란로 123")
            String streetAddress,

            @Schema(description = "우편번호", example = "12345")
            String postalCode,

            @Schema(description = "상세 주소", example = "101호")
            String detailAddress,

            @Schema(description = "전화번호", example = "02-1234-5678")
            String phoneNumber,

            @Schema(description = "전체 주소", example = "서울시 강남구 역삼동 테헤란로 123 101호")
            String fullAddress
    ) {
        public static BusinessAddressInfo from(AddressResponseDto address) {
            if (address == null) {
                return null;
            }
            return new BusinessAddressInfo(
                    address.getId(),
                    address.getTitle(),
                    address.getCity(),
                    address.getDistrict(),
                    address.getNeighborhood(),
                    address.getStreetAddress(),
                    address.getPostalCode(),
                    address.getDetailAddress(),
                    address.getPhoneNumber(),
                    address.getFullAddress()
            );
        }
    }

    public static SellerInfoResponseDTO from(Sellers seller, AddressResponseDto businessAddress) {
        if (seller == null) {
            return null;
        }

        return new SellerInfoResponseDTO(
                seller.getUserId() != null ? seller.getUserId() : null,
                seller.getVendorName(),
                seller.getVendorProfileImage(),
                seller.getBusinessNumber(),
                seller.getSettlementBank(),
                seller.getSettlementAccount(),
                seller.getTags(),
                seller.getOperatingStartTime(),
                seller.getOperatingEndTime(),
                seller.getClosedDays(),
                seller.getDeliveryFee(),
                seller.getFreeShippingThreshold(),
                BusinessAddressInfo.from(businessAddress),
                seller.getCreatedAt(),
                seller.getUpdatedAt()
        );
    }

    // 기존 호환성을 위한 메서드
    public static SellerInfoResponseDTO from(Sellers seller) {
        return from(seller, null);
    }
}