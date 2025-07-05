package com.team5.catdogeats.admins.controller;

import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.service.*;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 관리 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Management", description = "관리자 관리 API")
public class AdminManagementController {

    private final AdminInvitationService invitationService;
    private final AdminVerificationService verificationService;
    private final AdminPasswordResetService passwordResetService;
    private final AdminControllerUtils controllerUtils;
    private final AdminManagementService managementService;
    private final AdminSoftDeleteService softDeleteService;

    @Value("${admin.super.email}")
    private String superAdminEmail;


    /**
     * 관리자 계정 관리 페이지 (ADMIN 부서만 접근 가능)
     */
    @GetMapping("/account-management")
    public String showAccountManagementPage(HttpSession session, Model model) {
        String redirectResult = controllerUtils.validatePageAccess(session, true);
        if (redirectResult != null) {
            return redirectResult;
        }

        AdminInfo sessionInfo = controllerUtils.requireAdminDepartment(session);
        model.addAttribute("admin", sessionInfo);
        // 환경변수로부터 슈퍼관리자 이메일을 모델에 추가
        model.addAttribute("superAdminEmail", superAdminEmail);
        return "thymeleaf/administratorPage_account_management";
    }

    /**
     * 관리자 목록 조회 API
     */
    @GetMapping("/accounts")
    @ResponseBody
    @Operation(summary = "관리자 목록 조회", description = "페이지네이션과 필터링이 적용된 관리자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<AdminListResponseDTO>> getAdminList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Department department,
            HttpSession session) {

        controllerUtils.requireAdminDepartment(session);
        AdminListResponseDTO response = managementService.getAdminList(page, size, status, search, department);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    /**
     * 관리자 초대 페이지 (ADMIN 부서만 접근 가능)
     */
    @GetMapping("/invite")
    public String showInvitePage(HttpSession session) {
        String redirectResult = controllerUtils.validatePageAccess(session, true);
        if (redirectResult != null) {
            return redirectResult;
        }
        return "thymeleaf/administratorPage_invite";
    }


    /**
     * 관리자 추가
     */
    @PostMapping("/invite")
    @ResponseBody
    @Operation(summary = "관리자 추가", description = "새로운 관리자를 초대합니다.")
    public ResponseEntity<ApiResponse<AdminInvitationResponseDTO>> inviteAdmin(
            @Valid @RequestBody AdminInvitationRequestDTO request,
            HttpSession session) {

        controllerUtils.requireAdminDepartment(session);
        AdminInvitationResponseDTO response = invitationService.inviteAdmin(request);

        log.info("관리자 추가 성공: 추가자={}, 피추가자={}",
                controllerUtils.getSessionUserInfo(session), request.email());

        return ResponseEntity.ok(ApiResponse.success(ResponseCode.CREATED, response));
    }

    /**
     * 계정 인증 페이지 (모든 사용자 접근 가능)
     */
    @GetMapping("/verify")
    public String showVerifyPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email != null ? email : "");
        return "thymeleaf/administratorPage_verify";
    }

    /**
     * 계정 인증 처리
     */
    @PostMapping("/verify")
    @ResponseBody
    @Operation(summary = "계정 인증", description = "인증코드를 통해 관리자 계정을 활성화합니다.")
    public ResponseEntity<ApiResponse<AdminVerificationResponseDTO>> verifyAdmin(
            @Valid @RequestBody AdminVerificationRequestDTO request) {

        AdminVerificationResponseDTO response = verificationService.verifyAdmin(request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    /**
     * 인증코드 재발송
     */
    @PostMapping("/resend-code")
    @ResponseBody
    @Operation(summary = "인증코드 재발송", description = "만료된 인증코드를 재발송합니다.")
    public ResponseEntity<ApiResponse<String>> resendVerificationCode(@RequestParam String email) {
        verificationService.resendVerificationCode(email);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, "인증코드가 재발송되었습니다."));
    }

    /**
     * 관리자 비밀번호 초기화 요청
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<AdminPasswordResetResponseDTO>> resetAdminPassword(
            @Valid @RequestBody AdminPasswordResetRequestDTO request,
            HttpSession session) {

        // 슈퍼관리자 체크
        if (superAdminEmail.equals(request.email())) {
            throw new IllegalArgumentException("슈퍼관리자 계정은 비밀번호를 초기화할 수 없습니다.");
        }

        // 권한 체크
        controllerUtils.requireAdminDepartment(session);

        // 서비스 호출
        AdminPasswordResetResponseDTO response = passwordResetService.requestPasswordReset(request);

        log.info("비밀번호 초기화 요청 성공: target={}, requestedBy={}",
                request.email(), request.requestedBy());

        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    /**
     * 비밀번호 재설정 처리
     */
    @PostMapping("/verify-reset-password")
    @ResponseBody
    @Operation(summary = "비밀번호 재설정", description = "인증코드 확인 후 새 비밀번호를 설정합니다.")
    public ResponseEntity<ApiResponse<AdminVerificationResponseDTO>> verifyAndResetPassword(
            @Valid @RequestBody AdminPasswordResetVerificationDTO request) {

        AdminVerificationResponseDTO response = passwordResetService.verifyAndResetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }


    /**
     * 관리자 퇴사 처리 (ADMIN 부서만 접근 가능)
     */
    @PostMapping("/accounts/soft-delete")
    @ResponseBody
    @Operation(summary = "관리자 퇴사 처리", description = "관리자를 소프트 딜리트 처리합니다.")
    public ResponseEntity<ApiResponse<AdminSoftDeleteResponseDTO>> softDeleteAdmin(
            @Valid @RequestBody AdminSoftDeleteRequestDTO request,
            HttpSession session) {

        // ADMIN 부서 권한 확인
        AdminInfo sessionInfo = controllerUtils.requireAdminDepartment(session);


        AdminSoftDeleteRequestDTO serviceRequest = new AdminSoftDeleteRequestDTO(
                request.targetEmail(),
                sessionInfo.email(),
                request.reason()
        );

        AdminSoftDeleteResponseDTO response = softDeleteService.softDeleteAdmin(serviceRequest);

        log.info("관리자 퇴사 처리 요청: target={}, requestedBy={}",
                request.targetEmail(), sessionInfo.email());

        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    /**
     * 관리자 퇴사 취소 (복구) (ADMIN 부서만 접근 가능)
     */
    @PostMapping("/restore")
    @ResponseBody
    @Operation(summary = "관리자 퇴사 취소", description = "퇴사 처리된 관리자를 복구합니다.")
    public ResponseEntity<ApiResponse<AdminSoftDeleteResponseDTO>> restoreAdmin(
            @Valid @RequestBody AdminRestoreRequestDTO request,  // 새로운 DTO
            HttpSession session) {

        AdminInfo sessionInfo = controllerUtils.requireAdminDepartment(session);

        AdminSoftDeleteResponseDTO response = softDeleteService.undoSoftDelete(request.email());

        log.info("관리자 퇴사 취소 요청: target={}, requestedBy={}",
                request.email(), sessionInfo.email());

        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }


}