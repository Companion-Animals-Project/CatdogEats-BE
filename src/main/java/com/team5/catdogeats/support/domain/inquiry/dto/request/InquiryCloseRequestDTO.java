package com.team5.catdogeats.support.domain.inquiry.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 1:1 문의 강제 종료 요청 DTO
 * - 관리자가 문의를 강제로 종료할 때 사용
 * - 악성 문의, 장기간 미응답, 스팸, 부적절한 내용 등을 차단할 때 사용
 */
@Schema(description = "문의 강제 종료 요청")
public record InquiryCloseRequestDTO(
        @Schema(description = "문의 ID")
        @NotBlank(message = "문의 ID는 필수입니다")
        String inquiryId,

        @Schema(description = "강제 종료 사유", example = "부적절한 내용으로 인한 강제 종료")
        @NotBlank(message = "강제 종료 시, 사유는 필수입니다")
        @Size(max = 200, message = "종료 사유는 200자 이내로 입력해주세요")
        String reason
) {
}