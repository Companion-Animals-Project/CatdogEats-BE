package com.team5.catdogeats.admins.controller;

import com.team5.catdogeats.admins.domain.dto.dashboard.DashboardResponseDTO;
import com.team5.catdogeats.admins.service.AdminDashboardService;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 대시보드 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/v1/admin/dashboard/api")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "관리자 대시보드 API")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final AdminControllerUtils controllerUtils;

    /**
     * 대시보드 데이터 조회
     */
    @GetMapping
    @Operation(summary = "대시보드 데이터 조회", description = "관리자 대시보드에 필요한 통계 데이터를 조회합니다.")
    public ResponseEntity<APIResponse<DashboardResponseDTO>> getDashboardData(HttpSession session) {

        // 세션 검증 (로그인 여부 확인)
        controllerUtils.requireSessionInfo(session);

        // 대시보드 데이터 조회
        DashboardResponseDTO dashboardData = dashboardService.getDashboardData();

        log.info("대시보드 데이터 조회 성공: {}", controllerUtils.getSessionUserInfo(session));

        return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, dashboardData));
    }
}