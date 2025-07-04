package com.team5.catdogeats.support.dto;

import com.team5.catdogeats.support.domain.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ReportStatusUpdateDto(
        @NotNull(message = "처리 상태는 필수입니다")
        ReportStatus reportStatus,

        @Size(max = 1000, message = "관리자 메모는 1000자 이내로 입력해주세요")
        String adminNote
) {}