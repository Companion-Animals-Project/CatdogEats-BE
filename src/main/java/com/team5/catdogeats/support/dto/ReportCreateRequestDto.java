package com.team5.catdogeats.support.dto;

import com.team5.catdogeats.support.domain.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ReportCreateRequestDto (
        @NotNull(message = "신고 유형은 필수입니다")
        ReportType reportType,

        @NotBlank(message = "신고 사유는 필수입니다")
        @Size(max = 255, message = "신고 사유는 255자 이내로 입력해주세요")
        String reason,

        @NotBlank(message = "신고 내용은 필수입니다")
        @Size(max = 1000, message = "신고 내용은 1000자 이내로 입력해주세요")
        String content,

        // 신고 대상 ID (상품 또는 리뷰)
        @NotBlank(message = "신고 대상 ID는 필수입니다")
        String targetId,

        // 첨부파일 (선택사항)
        String attachmentUrl
) {}