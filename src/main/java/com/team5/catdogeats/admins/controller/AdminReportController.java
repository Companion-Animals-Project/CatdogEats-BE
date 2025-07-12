package com.team5.catdogeats.admins.controller;

import com.team5.catdogeats.admins.domain.dto.AdminInfo;
import com.team5.catdogeats.admins.domain.dto.ReportDetailResponseDto;
import com.team5.catdogeats.admins.domain.dto.ReportSearchDto;
import com.team5.catdogeats.admins.domain.dto.ReportStatusUpdateDto;
import com.team5.catdogeats.admins.service.AdminReportService;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.dto.PageResponseDto;
import com.team5.catdogeats.support.domain.dto.ReportListResponseDto;
import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;

@Controller
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Report", description = "관리자 신고 관리 API")
public class AdminReportController {

    private final AdminReportService adminReportService;
    private final AdminControllerUtils controllerUtils;

    // 신고 관리 페이지 표시
    @GetMapping("/reports")
    public String showReportsPage(HttpSession session, Model model) {
        String redirectResult = controllerUtils.checkFirstLoginRedirect(session);
        if (redirectResult != null) {
            return redirectResult;
        }

        AdminInfo sessionInfo = controllerUtils.requireSessionInfo(session);
        model.addAttribute("admin", sessionInfo);
        return "thymeleaf/administratorPage_reportManage"; // 신고 관리 페이지 템플릿
    }

    // 신고 목록 조회 API
    @GetMapping("/reports/list")
    @ResponseBody
    @Operation(summary = "신고 목록 조회", description = "관리자가 신고 목록을 조회합니다.")
    public ResponseEntity<APIResponse<PageResponseDto<ReportListResponseDto>>> getReports(
            @RequestParam(required = false) String reportType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            HttpSession session) {

        try {
            // 세션에서 관리자 정보 조회
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);
            String adminId = adminInfo.adminId();

            log.info("관리자 신고 목록 조회: adminId={}, email={}, page={}, size={}",
                    adminId, adminInfo.email(), page, size);

            // 검색 조건 DTO 생성
            ReportSearchDto searchDto = ReportSearchDto.builder()
                    .reportType(reportType != null ? ReportType.valueOf(reportType.toUpperCase()) : null)
                    .status(status != null ? ReportStatus.valueOf(status.toUpperCase()) : null)
                    .keyword(keyword)
                    .startDate(startDate != null ? ZonedDateTime.parse(startDate + "T00:00:00Z") : null)
                    .endDate(endDate != null ? ZonedDateTime.parse(endDate + "T23:59:59Z") : null).page(page)
                    .size(size)
                    .sort(sort)
                    .build();

            PageResponseDto<ReportListResponseDto> reports = adminReportService.getReports(searchDto);

            return ResponseEntity.ok(
                    APIResponse.success(ResponseCode.REPORT_LIST_SUCCESS, reports)
            );

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 검색 조건: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("관리자 신고 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "신고 목록 조회 중 오류가 발생했습니다."));
        }
    }

    // 신고 상세 조회 API
    @GetMapping("/reports/{report-id}")
    @ResponseBody
    @Operation(summary = "신고 상세 조회", description = "관리자가 특정 신고의 상세 정보를 조회합니다.")
    public ResponseEntity<APIResponse<ReportDetailResponseDto>> getReportDetail(
            @PathVariable("report-id") String reportId,
            HttpSession session) {

        try {
            // 세션에서 관리자 정보 조회
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);
            String adminId = adminInfo.adminId();

            log.info("신고 상세 조회: reportId={}, adminId={}, email={}",
                    reportId, adminId, adminInfo.email());

            ReportDetailResponseDto report = adminReportService.getReportDetail(reportId);

            return ResponseEntity.ok(
                    APIResponse.success(ResponseCode.REPORT_DETAIL_SUCCESS, report)
            );

        } catch (EntityNotFoundException e) {
            log.warn("신고 상세 조회 실패 - 신고 없음: reportId={}", reportId);
            return ResponseEntity.status(ResponseCode.REPORT_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.REPORT_NOT_FOUND, e.getMessage()));

        } catch (Exception e) {
            log.error("신고 상세 조회 중 오류 발생: reportId={}", reportId, e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "신고 상세 조회 중 오류가 발생했습니다."));
        }
    }

    // 신고 상태 처리 API
    @PostMapping("/reports/status")
    @ResponseBody
    @Operation(summary = "신고 상태 변경", description = "관리자가 신고의 상태를 변경합니다.")
    public ResponseEntity<APIResponse<Void>> updateReportStatus(
            @RequestParam String reportId,
            @Valid @RequestBody ReportStatusUpdateDto updateDto,
            HttpSession session) {

        try {
            // 세션에서 관리자 정보 조회
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);
            String adminId = adminInfo.adminId();

            log.info("신고 상태 변경 요청: reportId={}, 상태={}, adminId={}, email={}",
                    reportId, updateDto.reportStatus(), adminId, adminInfo.email());

            adminReportService.updateReportStatus(reportId, updateDto, adminId);

            log.info("신고 상태 변경 완료: reportId={}, 새 상태={}", reportId, updateDto.reportStatus());

            return ResponseEntity.ok(
                    APIResponse.success(ResponseCode.REPORT_STATUS_UPDATE_SUCCESS)
            );

        } catch (EntityNotFoundException e) {
            log.warn("신고 상태 변경 실패 - 신고 없음: reportId={}", reportId);
            return ResponseEntity.status(ResponseCode.REPORT_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.REPORT_NOT_FOUND, e.getMessage()));

        } catch (Exception e) {
            log.error("신고 상태 변경 중 오류 발생: reportId={}", reportId, e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "신고 상태 변경 중 오류가 발생했습니다."));
        }
    }
}