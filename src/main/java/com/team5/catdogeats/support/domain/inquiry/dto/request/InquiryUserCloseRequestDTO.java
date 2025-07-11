package com.team5.catdogeats.support.domain.inquiry.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 사용자 문의 종료 요청 DTO
 * - 사용자가 본인의 문의를 종료할 때 사용
 * - 문의가 해결되었거나 더 이상 답변이 필요하지 않을 때 사용
 */
@Schema(description = "사용자 문의 종료 요청")
public record InquiryUserCloseRequestDTO(
        @Schema(description = "문의 ID", example = "inquiry-uuid-123")
        @NotBlank(message = "문의 ID는 필수입니다")
        String inquiryId
) {
}