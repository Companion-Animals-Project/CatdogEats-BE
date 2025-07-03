package com.team5.catdogeats.admins.controller;

import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.service.AdminAuthenticationService;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 관리자 인증 컨트롤러
 * 세션 기반 로그인/로그아웃 처리
 */
@Slf4j
@Controller
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Authentication", description = "관리자 인증 API")
public class AdminAuthController {

    private final AdminAuthenticationService authService;
    private final AdminControllerUtils controllerUtils;

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public String showLoginPage(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            // 이미 로그인된 상태라면 적절한 페이지로 리다이렉트
            AdminInfo sessionInfo = authService.getSessionInfo(session);
            if (sessionInfo != null && sessionInfo.isValid()) {
                log.debug("이미 로그인된 관리자: {}", sessionInfo.email());
                return sessionInfo.isFirstLogin()
                        ? "redirect:/v1/admin/change-password"
                        : "redirect:/v1/admin/dashboard";
            }
        }

        return "thymeleaf/administratorPage_login";
    }

    /**
     * 로그인 처리
     */
    @PostMapping("/login")
    @ResponseBody
    @Operation(summary = "관리자 로그인", description = "관리자 계정으로 로그인합니다.")
    public ResponseEntity<ApiResponse<AdminLoginResponseDTO>> login(
            @Valid @RequestBody AdminLoginRequestDTO request,
            HttpServletRequest servletRequest) {

        // 로그인 성공 시에만 세션 생성 (true 사용)
        HttpSession session = servletRequest.getSession(true);
        AdminLoginResponseDTO response = authService.login(request, session);

        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    /**
     * 로그아웃 처리
     */
    @PostMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        // getSession(false)로 기존 세션만 조회
        HttpSession session = request.getSession(false);

        if (session != null) {
            try {
                log.info("로그아웃 시작: sessionId={}", session.getId());
                authService.logout(session);
                clearAllCookies(request, response);
                log.info("로그아웃 완료: sessionId={}", session.getId());
            } catch (Exception e) {
                log.error("로그아웃 중 오류 발생", e);
                // 오류 발생 시에도 쿠키는 삭제
                clearAllCookies(request, response);
            }
        } else {
            log.debug("로그아웃 요청이지만 세션이 없음");
            // 세션이 없어도 쿠키는 삭제
            clearAllCookies(request, response);
        }

        return "redirect:/v1/admin/login?logout=true";
    }

    /**
     * 비밀번호 변경 페이지
     */
    @GetMapping("/change-password")
    public String showChangePasswordPage(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/v1/admin/login";
        }

        AdminInfo sessionInfo = controllerUtils.requireSessionInfo(session);
        model.addAttribute("adminName", sessionInfo.name());
        model.addAttribute("isFirstLogin", sessionInfo.isFirstLogin());
        return "thymeleaf/administratorPage_change_password";
    }

    /**
     * 비밀번호 변경 처리
     */
    @PostMapping("/change-password")
    @ResponseBody
    @Operation(summary = "관리자 비밀번호 변경", description = "관리자의 비밀번호를 변경합니다.")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody AdminPasswordChangeRequestDTO request,
            HttpServletRequest servletRequest) {

        HttpSession session = servletRequest.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(ResponseCode.UNAUTHORIZED, "로그인이 필요합니다."));
        }

        authService.changePassword(request, session);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, "비밀번호가 성공적으로 변경되었습니다."));
    }

    /**
     * 대시보드 페이지
     */
    @GetMapping("/dashboard")
    public String showDashboard(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/v1/admin/login";
        }

        String redirectCheck = controllerUtils.checkFirstLoginRedirect(session);
        if (redirectCheck != null) {
            return redirectCheck;
        }

        AdminInfo sessionInfo = controllerUtils.requireSessionInfo(session);
        model.addAttribute("admin", sessionInfo);
        return "thymeleaf/administratorPage_dashboard";
    }

    /**
     * 관리자 정보 조회
     */
    @GetMapping("/profile")
    @ResponseBody
    @Operation(summary = "관리자 정보 조회", description = "현재 로그인한 관리자의 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<AdminInfo>> getAdminProfile(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(ResponseCode.UNAUTHORIZED, "로그인이 필요합니다."));
        }

        AdminInfo sessionInfo = controllerUtils.requireSessionInfo(session);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, sessionInfo));
    }

    /**
     * 모든 쿠키 삭제
     */
    private void clearAllCookies(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String cookieName = cookie.getName();
                log.debug("쿠키 삭제 시도: {}", cookieName);

                Cookie deleteCookie = new Cookie(cookieName, null);
                deleteCookie.setMaxAge(0);
                deleteCookie.setPath("/");
                deleteCookie.setHttpOnly(true);

                if (cookie.getDomain() != null) {
                    deleteCookie.setDomain(cookie.getDomain());
                }

                response.addCookie(deleteCookie);

                // 루트 경로에서도 삭제 시도
                if (!"/".equals(cookie.getPath())) {
                    Cookie rootDeleteCookie = new Cookie(cookieName, null);
                    rootDeleteCookie.setMaxAge(0);
                    rootDeleteCookie.setPath("/");
                    rootDeleteCookie.setHttpOnly(true);
                    if (cookie.getDomain() != null) {
                        rootDeleteCookie.setDomain(cookie.getDomain());
                    }
                    response.addCookie(rootDeleteCookie);
                }
            }
        }

        // 알려진 세션 관련 쿠키들 명시적으로 삭제
        clearSpecificCookie(response, "JSESSIONID");
        clearSpecificCookie(response, "SESSION");
    }

    /**
     * 특정 쿠키 삭제
     */
    private void clearSpecificCookie(HttpServletResponse response, String cookieName) {
        Cookie cookie = new Cookie(cookieName, null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        log.debug("특정 쿠키 삭제: {}", cookieName);
    }


}
