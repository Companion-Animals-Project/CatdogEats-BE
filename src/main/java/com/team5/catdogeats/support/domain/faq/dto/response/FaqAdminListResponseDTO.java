package com.team5.catdogeats.support.domain.faq.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.team5.catdogeats.support.domain.enums.FaqCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * 관리자 FAQ 목록 응답 DTO
 * - 관리자 페이지의 FAQ 목록에서 사용
 * - 간략한 정보만 표시
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "관리자 FAQ 목록 응답")
public class FaqAdminListResponseDTO {

    @Schema(description = "FAQ ID")
    private String id;

    @Schema(description = "질문")
    private String question;

    @Schema(description = "카테고리", example = "ACCOUNT")
    private FaqCategory faqCategory;

    @Schema(description = "카테고리 표시명")
    private String categoryDisplayName;

    @Schema(description = "노출 순서", example = "1")
    private int displayOrder;

    @Schema(description = "등록일")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private ZonedDateTime createdAt;

    @Schema(description = "키워드 목록")
    private List<String> keywords;
}