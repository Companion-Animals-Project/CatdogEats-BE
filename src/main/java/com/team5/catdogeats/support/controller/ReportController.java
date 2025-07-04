package com.team5.catdogeats.support.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import com.team5.catdogeats.support.dto.*;
import com.team5.catdogeats.support.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * 신고하기 (구매자용)
     */
    @PostMapping("/buyers/reports")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<String>> createReport(
            @Valid @RequestBody ReportCreateRequestDto request,
            HttpServletRequest httpRequest) {

        // TODO: JWT에서 사용자 ID 추출하는 유틸리티 메서드 사용
        String reporterId = getCurrentUserId(httpRequest);

        log.info("신고 생성 요청: 사용자={}, 타입={}", reporterId, request.reportType());

        String reportId = reportService.createReport(request, reporterId);

        return ResponseEntity.ok(
                ApiResponse.success(ResponseCode.CREATED, reportId)
        );
    }

    /**
     * 사용자별 신고 목록 조회 (구매자용)
     */
    @GetMapping("/buyers/reports")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<PageResponseDto<ReportListResponseDto>>> getUserReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        String userId = getCurrentUserId(httpRequest);

        PageResponseDto<ReportListResponseDto> reports =
                reportService.getUserReports(userId, page, size);

        return ResponseEntity.ok(
                ApiResponse.success(ResponseCode.SUCCESS, reports)
        );
    }

    /**
     * 신고 목록 조회 (관리자용)
     */
    @GetMapping("/admin/reports/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponseDto<ReportListResponseDto>>> getReports(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort) {

        // 검색 조건 DTO 생성
        ReportSearchDto searchDto = ReportSearchDto.builder()
                .reportType(reportType != null ? ReportType.valueOf(reportType.toUpperCase()) : null)
                .status(status != null ? ReportStatus.valueOf(status.toUpperCase()) : null)
                .keyword(keyword)
                .startDate(startDate != null ? LocalDate.parse(startDate) : null)
                .endDate(endDate != null ? LocalDate.parse(endDate) : null)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        PageResponseDto<ReportListResponseDto> reports = reportService.getReports(searchDto);

        return ResponseEntity.ok(
                ApiResponse.success(ResponseCode.SUCCESS, reports)
        );
    }

    /**
     * 신고 상세 조회 (관리자용)
     */
    @GetMapping("/admin/reports/{report-id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportDetailResponseDto>> getReportDetail(
            @PathVariable("report-id") String reportId) {

        ReportDetailResponseDto report = reportService.getReportDetail(reportId);

        return ResponseEntity.ok(
                ApiResponse.success(ResponseCode.SUCCESS, report)
        );
    }

    /**
     * 신고 상태 처리 (관리자용)
     */
    @PostMapping("/admin/reports/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateReportStatus(
            @RequestParam String reportId,
            @Valid @RequestBody ReportStatusUpdateDto updateDto,
            HttpServletRequest httpRequest) {

        // TODO: 관리자 ID 추출
        String adminId = getCurrentAdminId(httpRequest);

        log.info("신고 상태 변경 요청: ID={}, 상태={}, 관리자={}",
                reportId, updateDto.status(), adminId);

        reportService.updateReportStatus(reportId, updateDto, adminId);

        return ResponseEntity.ok(
                ApiResponse.success(ResponseCode.SUCCESS)
        );
    }

    /**
     * 신고 통계 조회 (관리자 대시보드용)
     */
    @GetMapping("/admin/reports/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportStatsResponseDto>> getReportStats() {

        ReportStatsResponseDto stats = reportService.getReportStats();

        return ResponseEntity.ok(
                ApiResponse.success(ResponseCode.SUCCESS, stats)
        );
    }

    // === 헬퍼 메서드 ===

    /**
     * 현재 로그인한 사용자 ID 추출
     * TODO: JWT 유틸리티 클래스에서 구현된 메서드 사용
     */
    private String getCurrentUserId(HttpServletRequest request) {
        // 임시 구현 - 실제로는 JWT에서 추출
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // JWT 파싱 로직 구현 필요
            return "temp-user-id";
        }
        throw new IllegalStateException("인증 정보가 없습니다.");
    }

    /**
     * 현재 로그인한 관리자 ID 추출
     * TODO: 세션에서 관리자 정보 추출
     */
    private String getCurrentAdminId(HttpServletRequest request) {
        // 임시 구현 - 실제로는 세션에서 추출
        return "temp-admin-id";
    }
}