package com.team5.catdogeats.support.domain.inquiry.dto;

import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


// 관리자 답변 등록 요청 DTO
public record InquiryReplyRequestDTO(
        @NotBlank(message = "답변 내용은 필수입니다")
        @Size(max = 2000, message = "답변 내용은 2,000자를 초과할 수 없습니다")
        String content,

        @Schema(
                description = "긴급도",
                example = "MEDIUM",
                allowableValues = {"HIGH", "MEDIUM", "LOW"},
                enumAsRef = true
        )
        @NotNull(message = "긴급도는 필수입니다")
        InquiryUrgentLevel urgentLevel) {
}
