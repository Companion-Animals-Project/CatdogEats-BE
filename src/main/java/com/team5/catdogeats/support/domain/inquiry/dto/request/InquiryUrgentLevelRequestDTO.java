package com.team5.catdogeats.support.domain.inquiry.dto.request;

import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 1:1 문의 긴급도 수정 요청 DTO
 * - 관리자가 문의의 긴급도를 별도로 수정할 때 사용
 */
@Schema(description = "문의 긴급도 수정 요청")
public record InquiryUrgentLevelRequestDTO(
        @Schema(description = "문의 ID", example = "inquiry-uuid-123")
        @NotBlank(message = "문의 ID는 필수입니다")
        String inquiryId,

        @Schema(
                description = "긴급도 레벨",
                example = "HIGH",
                allowableValues = {"HIGH", "MIDDLE", "LOW"},
                enumAsRef = true
        )
        @NotNull(message = "긴급도는 필수입니다")
        InquiryUrgentLevel urgentLevel
) {
}