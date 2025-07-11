package com.team5.catdogeats.support.domain.inquiry.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * 1:1 문의 답변/답글 등록 요청 DTO
 * - 관리자 답변 등록 (이미지 + 문서 파일 첨부 가능)
 * - 사용자 추가 문의 등록
 */
@Schema(description = "문의 답변/답글 등록 요청")
public record InquiryReplyRequestDTO(
        @Schema(description = "문의 ID", example = "inquiry-uuid-123")
        @NotBlank(message = "문의 ID는 필수입니다")
        String inquiryId,

        @Schema(description = "답변/답글 내용", example = "문의 주신 내용에 대한 답변입니다.")
        @NotBlank(message = "답변 내용은 필수입니다")
        @Size(min = 5, max = 2000, message = "답변 내용은 5자 이상 2,000자 이하로 입력해주세요")
        String content,

        @Schema(description = "첨부 이미지 파일들 (선택사항)",
                example = "null",
                nullable = true)
        MultipartFile[] imageFiles,

        @Schema(description = "첨부 문서 파일들 (선택사항)",
                example = "null",
                nullable = true)
        MultipartFile[] documentFiles
) {
}