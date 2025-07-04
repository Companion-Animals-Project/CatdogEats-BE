package com.team5.catdogeats.admins.domain.dto;

import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record ReportDetailResponseDto(
        String reportId,
        ReportType reportType,
        String reason,
        String content,
        ReportStatus reportStatus,
        String reporterName,
        String reporterId,
        String targetId,
        String targetName,
        String targetDescription,
        String attachmentUrl,
        String adminNote,
        String processedByAdminId,
        ZonedDateTime createdAt,
        ZonedDateTime processedAt

) {}
