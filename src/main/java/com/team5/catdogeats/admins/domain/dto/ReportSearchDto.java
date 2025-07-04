package com.team5.catdogeats.admins.domain.dto;

import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ReportSearchDto(
        ReportType reportType,
        ReportStatus status,
        String keyword,
        LocalDate startDate,
        LocalDate endDate,
        int page,
        int size,
        String sort
) {}