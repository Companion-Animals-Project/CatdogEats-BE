package com.team5.catdogeats.support.domain.dto;

import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record ReportListResponseDto(
        String reportId,
        ReportType reportType,
        String reason,
        ReportStatus reportStatus,
        String reporterName,
        String reporterId,
        String targetId,
        String targetName,
        ZonedDateTime createdAt
) {}