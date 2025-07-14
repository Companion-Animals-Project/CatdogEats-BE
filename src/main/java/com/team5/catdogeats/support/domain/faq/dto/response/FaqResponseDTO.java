package com.team5.catdogeats.support.domain.faq.dto.response;

import com.team5.catdogeats.support.domain.enums.FaqCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 퍼블릭 FAQ 응답 DTO
 * - 사용자가 FAQ 목록을 조회할 때 사용
 * - 토글 형태로 질문/답변 표시
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "퍼블릭 FAQ 응답")
public class FaqResponseDTO {

    @Schema(description = "FAQ ID")
    private String id;

    @Schema(description = "질문")
    private String question;

    @Schema(description = "답변")
    private String answer;

    @Schema(description = "카테고리", example = "ACCOUNT")
    private FaqCategory faqCategory;

    @Schema(description = "카테고리 표시명")
    private String categoryDisplayName;

    @Schema(description = "키워드 목록")
    private List<String> keywords;
}
