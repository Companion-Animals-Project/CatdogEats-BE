package com.team5.catdogeats.admins.service;

import com.team5.catdogeats.admins.domain.dto.ReportDetailResponseDto;
import com.team5.catdogeats.admins.domain.dto.ReportSearchDto;
import com.team5.catdogeats.admins.domain.dto.ReportStatsResponseDto;
import com.team5.catdogeats.admins.domain.dto.ReportStatusUpdateDto;
import com.team5.catdogeats.support.domain.dto.PageResponseDto;
import com.team5.catdogeats.support.domain.dto.ReportListResponseDto;

public interface AdminReportService {
    // 신고 목록 조회 (관리자용)
    PageResponseDto<ReportListResponseDto> getReports(ReportSearchDto searchDto);

    // 신고 상세 조회
    ReportDetailResponseDto getReportDetail(String reportId);

    // 신고 상태 변경 (관리자)
    void updateReportStatus(String reportId, ReportStatusUpdateDto updateDto, String adminId);
}
