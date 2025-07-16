package com.team5.catdogeats.support.domain.notice.controller;

import com.team5.catdogeats.admins.domain.dto.AdminInfo;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;  // 이 줄로 변경
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/v1/admin/notice")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminPageController {

    private final AdminControllerUtils controllerUtils;

    // ========== 관리자 공지사항 페이지 표시만 담당 ==========
    @GetMapping
    public String showNoticeManagePage(HttpSession session, Model model) {
        AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);
        log.info("관리자 공지사항 페이지 접근 - adminId: {}, adminName: {}",
                adminInfo.adminId(), adminInfo.name());

        model.addAttribute("adminInfo", adminInfo);
        return "thymeleaf/administratorPage_noticeManage";
    }
}