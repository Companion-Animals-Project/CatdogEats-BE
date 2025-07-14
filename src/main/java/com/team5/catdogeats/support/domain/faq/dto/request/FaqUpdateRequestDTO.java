package com.team5.catdogeats.support.domain.faq.dto.request;

import com.team5.catdogeats.support.domain.enums.FaqCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FAQ 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "FAQ 수정 요청")
public class FaqUpdateRequestDTO {

    @Size(max = 100, message = "질문은 100자 이하로 입력해주세요")
    @Schema(description = "질문 (선택사항 - null 이면 수정 안함)")
    private String question;

    @Schema(description = "답변 (선택사항 - null 이면 수정 안함)")
    private String answer;

    @Schema(description = "FAQ 카테고리 (선택사항 - null 이면 수정 안함)", example = "ACCOUNT")
    private FaqCategory faqCategory;

    @Min(value = 1, message = "노출 순서는 1 이상이어야 합니다")
    @Schema(description = "노출 순서 (선택사항 - null 이면 수정 안함)", example = "1")
    private Integer displayOrder;

    @Schema(description = "키워드 목록 (선택사항 - null 이면 수정 안함)")
    private List<String> keywords;
}