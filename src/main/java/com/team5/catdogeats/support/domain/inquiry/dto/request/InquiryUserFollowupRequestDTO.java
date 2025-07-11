package com.team5.catdogeats.support.domain.inquiry.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 문의 답글 등록 요청 DTO
 * - 사용자가 기존 문의나 관리자 답변에 대한 추가 답글을 등록할 때 사용
 * - 이미지 파일 첨부 가능
 */
@Schema(description = "사용자 문의 답글 등록 요청")
public record InquiryUserFollowupRequestDTO(
        @Schema(description = "문의 ID", example = "inquiry-uuid-123")
        @NotBlank(message = "문의 ID는 필수입니다")
        String inquiryId,

        @Schema(description = "답글 내용", example = "추가로 문의드릴 내용이 있습니다.")
        @NotBlank(message = "내용은 필수입니다")
        @Size(min = 5, max = 2000, message = "내용은 5자 이상 2,000자 이하로 입력해주세요")
        String content,

        @Schema(description = "첨부 이미지 파일들 (선택사항, 최대 5개)",
                example = "null",
                nullable = true)
        MultipartFile[] imageFiles
) {
}