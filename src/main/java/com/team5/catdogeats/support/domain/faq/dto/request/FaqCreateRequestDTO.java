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
 * FAQ 등록 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "FAQ 등록 요청")
public class FaqCreateRequestDTO {

    @NotBlank(message = "질문은 필수입니다")
    @Size(max = 100, message = "질문은 100자 이하로 입력해주세요")
    @Schema(description = "질문")
    private String question;

    @NotBlank(message = "답변은 필수입니다")
    @Schema(description = "답변")
    private String answer;

    @NotNull(message = "카테고리는 필수입니다")
    @Schema(description = "FAQ 카테고리", example = "ACCOUNT")
    private FaqCategory faqCategory;

    @Min(value = 1, message = "노출 순서는 1 이상이어야 합니다")
    @Schema(description = "노출 순서", example = "1")
    private int displayOrder;

    @Schema(description = "키워드 목록")
    private List<String> keywords;
}