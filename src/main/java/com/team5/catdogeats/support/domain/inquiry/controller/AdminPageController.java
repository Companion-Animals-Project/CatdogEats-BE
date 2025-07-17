package com.team5.catdogeats.support.domain.inquiry.controller;

import com.team5.catdogeats.admins.domain.dto.AdminInfo;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller("inquiryAdminPageController")
@RequestMapping("/v1/admin/inquiry")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPageController {

    private final AdminControllerUtils adminControllerUtils;

    /**
     * 1:1 문의 관리 페이지
     */
    @GetMapping
    public String inquiryManagePage(HttpSession session, Model model) {
        try {
            // 세션에서 관리자 정보 확인
            AdminInfo adminInfo = adminControllerUtils.requireSessionInfo(session);

            // 관리자 정보를 모델에 추가 (헤더에 표시용)
            model.addAttribute("adminInfo", adminInfo);
            model.addAttribute("adminEmail", adminInfo.email());
            model.addAttribute("adminName", adminInfo.name());

            log.info("관리자 1:1 문의 관리 페이지 접근 - adminId: {}, adminName: {}",
                    adminInfo.adminId(), adminInfo.name());

            // Thymeleaf 템플릿 경로 반환
            return "thymeleaf/administratorPage_otoManage";

        } catch (Exception e) {
            log.error("관리자 페이지 접근 중 오류", e);
            // 오류 발생 시 로그인 페이지로 리다이렉트
            return "redirect:/v1/admin/login";
        }
    }
}