package com.team5.catdogeats.support.service;

import com.team5.catdogeats.support.dto.*;


public interface ReportService {

    // 신고 생성
    String createReport(ReportCreateRequestDto request, String reporterId);

    // 신고 목록 조회 (관리자용)
    PageResponseDto<ReportListResponseDto> getReports(ReportSearchDto searchDto);

    // 신고 상세 조회
    ReportDetailResponseDto getReportDetail(String reportId);

    // 신고 상태 변경 (관리자)
    void updateReportStatus(String reportId, ReportStatusUpdateDto updateDto, String adminId);

    // 사용자별 신고 목록 조회
    PageResponseDto<ReportListResponseDto> getUserReports(String userId, int page, int size);

    // 신고 통계 조회 (관리자 대시보드용)
    ReportStatsResponseDto getReportStats();
}