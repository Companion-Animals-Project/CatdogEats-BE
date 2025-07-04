package com.team5.catdogeats.support.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import com.team5.catdogeats.support.dto.*;
import com.team5.catdogeats.support.service.ReportService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    // 신고하기 (구매자용)
    @PostMapping("/buyers/reports")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<String>> createReport(
            @Valid @RequestBody ReportCreateRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            // UserPrincipal에서 실제 사용자 ID 추출
            String reporterId = getUserIdFromPrincipal(userPrincipal);

            log.info("신고 생성 요청: 사용자={}, 타입={}, 대상ID={}",
                    reporterId, request.reportType(), request.targetId());

            String reportId = reportService.createReport(request, reporterId);

            log.info("신고 생성 완료: reportId={}", reportId);

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.REPORT_CREATE_SUCCESS, reportId)
            );

        } catch (EntityNotFoundException e) {
            log.warn("신고 생성 실패 - 엔티티 없음: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("신고 생성 실패 - 중복 신고: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.REPORT_ALREADY_EXISTS.getStatus())
                    .body(ApiResponse.error(ResponseCode.REPORT_ALREADY_EXISTS, e.getMessage()));

        } catch (Exception e) {
            log.error("신고 생성 중 오류 발생", e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "신고 처리 중 오류가 발생했습니다."));
        }
    }

    // 사용자별 신고 목록 조회 (구매자용)
    @GetMapping("/buyers/reports")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<PageResponseDto<ReportListResponseDto>>> getUserReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            String userId = getUserIdFromPrincipal(userPrincipal);

            log.info("사용자 신고 목록 조회: userId={}, page={}, size={}", userId, page, size);

            PageResponseDto<ReportListResponseDto> reports =
                    reportService.getUserReports(userId, page, size);

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.REPORT_LIST_SUCCESS, reports)
            );

        } catch (Exception e) {
            log.error("사용자 신고 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "신고 목록 조회 중 오류가 발생했습니다."));
        }
    }

    // 신고 목록 조회 (관리자용)
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
            @RequestParam(defaultValue = "createdAt") String sort,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            String adminId = getUserIdFromPrincipal(userPrincipal);

            log.info("관리자 신고 목록 조회: adminId={}, page={}, size={}", adminId, page, size);

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
                    ApiResponse.success(ResponseCode.REPORT_LIST_SUCCESS, reports)
            );

        } catch (IllegalArgumentException e) {
            log.warn("잘못된 검색 조건: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("관리자 신고 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "신고 목록 조회 중 오류가 발생했습니다."));
        }
    }

    // 신고 상세 조회 (관리자용)
    @GetMapping("/admin/reports/{report-id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportDetailResponseDto>> getReportDetail(
            @PathVariable("report-id") String reportId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            String adminId = getUserIdFromPrincipal(userPrincipal);

            log.info("신고 상세 조회: reportId={}, adminId={}", reportId, adminId);

            ReportDetailResponseDto report = reportService.getReportDetail(reportId);

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.REPORT_DETAIL_SUCCESS, report)
            );

        } catch (EntityNotFoundException e) {
            log.warn("신고 상세 조회 실패 - 신고 없음: reportId={}", reportId);
            return ResponseEntity.status(ResponseCode.REPORT_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.REPORT_NOT_FOUND, e.getMessage()));

        } catch (Exception e) {
            log.error("신고 상세 조회 중 오류 발생: reportId={}", reportId, e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "신고 상세 조회 중 오류가 발생했습니다."));
        }
    }

    // 신고 상태 처리 (관리자용)
    @PostMapping("/admin/reports/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateReportStatus(
            @RequestParam String reportId,
            @Valid @RequestBody ReportStatusUpdateDto updateDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            String adminId = getUserIdFromPrincipal(userPrincipal);

            log.info("신고 상태 변경 요청: reportId={}, 상태={}, adminId={}",
                    reportId, updateDto.reportStatus(), adminId);

            reportService.updateReportStatus(reportId, updateDto, adminId);

            log.info("신고 상태 변경 완료: reportId={}, 새 상태={}", reportId, updateDto.reportStatus());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.REPORT_STATUS_UPDATE_SUCCESS)
            );

        } catch (EntityNotFoundException e) {
            log.warn("신고 상태 변경 실패 - 신고 없음: reportId={}", reportId);
            return ResponseEntity.status(ResponseCode.REPORT_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.REPORT_NOT_FOUND, e.getMessage()));

        } catch (Exception e) {
            log.error("신고 상태 변경 중 오류 발생: reportId={}", reportId, e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "신고 상태 변경 중 오류가 발생했습니다."));
        }
    }

    // 신고 통계 조회 (관리자 대시보드용)
    @GetMapping("/admin/reports/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ReportStatsResponseDto>> getReportStats(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            String adminId = getUserIdFromPrincipal(userPrincipal);

            log.info("신고 통계 조회: adminId={}", adminId);

            ReportStatsResponseDto stats = reportService.getReportStats();

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.REPORT_STATS_SUCCESS, stats)
            );

        } catch (Exception e) {
            log.error("신고 통계 조회 중 오류 발생", e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "신고 통계 조회 중 오류가 발생했습니다."));
        }
    }

    // === 헬퍼 메서드 ===

    // provider + providerId 조합으로 Users 테이블에서 사용자 조회
    private String getUserIdFromPrincipal(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        log.debug("사용자 조회: provider={}, providerId={}",
                userPrincipal.provider(), userPrincipal.providerId());

        // provider + providerId로 사용자 조회
        Users user = userRepository.findByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        ).orElseThrow(() -> {
            log.warn("사용자를 찾을 수 없음: provider={}, providerId={}",
                    userPrincipal.provider(), userPrincipal.providerId());
            return new EntityNotFoundException(
                    String.format("사용자를 찾을 수 없습니다. (provider: %s, providerId: %s)",
                            userPrincipal.provider(), userPrincipal.providerId())
            );
        });

        log.debug("사용자 조회 성공: userId={}, name={}", user.getId(), user.getName());
        return user.getId();
    }
}