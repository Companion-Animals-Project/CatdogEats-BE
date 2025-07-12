package com.team5.catdogeats.support.domain.inquiry.dto.request;

import com.team5.catdogeats.support.domain.enums.InquiryReceiveMethod;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * 1:1 문의 등록 요청 DTO
 * - 이미지 파일 첨부 가능
 */
@Schema(description = "1:1 문의 등록 요청")
public record InquiryCreateRequestDTO(
        @Schema(
                description = "문의 유형",
                example = "PRODUCT",
                allowableValues = {"PRODUCT", "ORDER", "PAYMENT", "DELIVERY", "RETURN", "ACCOUNT", "ETC"},
                enumAsRef = true
        )
        @NotNull(message = "문의 유형은 필수입니다")
        InquiryType inquiryType,

        @Schema(description = "제목")
        @NotBlank(message = "제목은 필수입니다")
        @Size(min = 5, max = 100, message = "제목은 5자 이상 100자 이하로 입력해주세요")
        String title,

        @Schema(description = "내용")
        @NotBlank(message = "내용은 필수입니다")
        @Size(min = 10, max = 2000, message = "내용은 10자 이상 2,000자 이하로 입력해주세요")
        String content,

        @Schema(description = "주문 ID (선택사항, 주문 관련 문의시 입력)",
                example = "null",
                nullable = true,
                defaultValue = "null")
        String orderId,

        @Schema(
                description = "답변 수신 방법",
                example = "WEB",
                allowableValues = {"WEB", "CALL", "SMS", "NONE"},
                defaultValue = "WEB",
                enumAsRef = true
        )
        InquiryReceiveMethod inquiryReceiveMethod,

        @Schema(hidden = true)
        MultipartFile[] imageFiles
) {
    public InquiryCreateRequestDTO {
        // orderId 정리 - null, 빈 문자열, "null" 문자열을 모두 null로 통일
        if (orderId != null) {
            orderId = orderId.trim();
            if (orderId.isEmpty() || orderId.equalsIgnoreCase("null") || orderId.equals("undefined")) {
                orderId = null;
            }
        }

        // 기본값 설정
        if (inquiryReceiveMethod == null) {
            inquiryReceiveMethod = InquiryReceiveMethod.WEB;
        }
    }
}