package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

@Schema(description = "판매자 정보 등록/수정 요청 DTO")
public record SellerInfoRequestDTO(

        @Schema(description = "업체명", example = "멍멍이네 수제간식", required = true)
        @Size(max = 100, message = "업체명은 최대 100자까지 입력 가능합니다.")
        String vendorName,

        @Schema(description = "사업자 등록번호", example = "123-45-67890", required = true)
        @Size(max = 20, message = "사업자 등록번호는 최대 20자까지 입력 가능합니다.")
        @Pattern(regexp = "^[0-9\\-]+$", message = "사업자 등록번호는 숫자와 하이픈만 입력 가능합니다.")
        String businessNumber,

        @Schema(description = "정산 은행명", example = "국민은행")
        @Size(max = 50, message = "은행명은 최대 50자까지 입력 가능합니다.")
        String settlementBank,

        @Schema(description = "정산 계좌번호", example = "123456789012")
        @Size(max = 30, message = "계좌번호는 최대 30자까지 입력 가능합니다.")
        String settlementAcc,

        @Schema(description = "태그 (쉼표로 구분)", example = "수제간식,강아지")
        @Size(max = 36, message = "태그는 최대 36자까지 입력 가능합니다.")
        String tags,

        @Schema(description = "운영 시작 시간", example = "09:00:00")
        LocalTime operatingStartTime,

        @Schema(description = "운영 종료 시간", example = "18:00:00")
        LocalTime operatingEndTime,

        @Schema(description = "휴무일 (쉼표로 구분)", example = "월요일,화요일")
        @Size(max = 20, message = "휴무일은 최대 20자까지 입력 가능합니다.")
        String closedDays,

        // 배송비 관련 필드 추가
        @Schema(description = "기본 배송비", example = "3000")
        @Min(value = 0, message = "배송비는 0원 이상이어야 합니다.")
        Long deliveryFee,

        @Schema(description = "무료배송 최소 주문금액", example = "50000")
        @Min(value = 0, message = "무료배송 최소 주문금액은 0원 이상이어야 합니다.")
        Long freeShippingThreshold,

        // 사업자 주소 정보
        @Schema(description = "사업자 주소 제목", example = "본사")
        @Size(max = 30, message = "주소 제목은 30자 이하여야 합니다")
        String addressTitle,

        @Schema(description = "시/도", example = "서울시")
        @Size(max = 100, message = "시/도는 100자 이하여야 합니다")
        String city,

        @Schema(description = "시/군/구", example = "강남구")
        @Size(max = 100, message = "시/군/구는 100자 이하여야 합니다")
        String district,

        @Schema(description = "읍/면/동", example = "역삼동")
        @Size(max = 100, message = "읍/면/동은 100자 이하여야 합니다")
        String neighborhood,

        @Schema(description = "도로명 주소", example = "테헤란로 123")
        @Size(max = 200, message = "도로명 주소는 200자 이하여야 합니다")
        String streetAddress,

        @Schema(description = "우편번호", example = "12345")
        @Size(max = 20, message = "우편번호는 20자 이하여야 합니다")
        String postalCode,

        @Schema(description = "상세 주소", example = "101호")
        @Size(max = 200, message = "상세 주소는 200자 이하여야 합니다")
        String detailAddress,

        @Schema(description = "전화번호", example = "02-1234-5678")
        @Size(max = 30, message = "전화번호는 30자 이하여야 합니다")
        String phoneNumber

) {
        public boolean isCreateRequest() {
                return vendorName != null && !vendorName.trim().isEmpty() &&
                        businessNumber != null && !businessNumber.trim().isEmpty();
        }

        // 주소 정보가 포함되어 있는지 확인
        public boolean hasAddressInfo() {
                return addressTitle != null && !addressTitle.trim().isEmpty() &&
                        city != null && !city.trim().isEmpty() &&
                        district != null && !district.trim().isEmpty() &&
                        neighborhood != null && !neighborhood.trim().isEmpty() &&
                        streetAddress != null && !streetAddress.trim().isEmpty() &&
                        postalCode != null && !postalCode.trim().isEmpty() &&
                        detailAddress != null && !detailAddress.trim().isEmpty() &&
                        phoneNumber != null && !phoneNumber.trim().isEmpty();
        }
}