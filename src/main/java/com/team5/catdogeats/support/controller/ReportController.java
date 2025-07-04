package com.team5.catdogeats.support.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.dto.PageResponseDto;
import com.team5.catdogeats.support.domain.dto.ReportCreateRequestDto;
import com.team5.catdogeats.support.domain.dto.ReportListResponseDto;
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

@RestController
@RequestMapping("/v1/buyers")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    // 신고하기
    @PostMapping("/reports")
    @PreAuthorize("hasRole('BUYER')")
    public ResponseEntity<ApiResponse<String>> createReport(
            @Valid @RequestBody ReportCreateRequestDto request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
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
    @GetMapping("/reports")
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



    // === 헬퍼 메서드 ===

    // provider + providerId로 Users 테이블에서 사용자 조회
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