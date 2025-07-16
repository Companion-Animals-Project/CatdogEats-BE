package com.team5.catdogeats.support.domain.notice.controller;

import com.team5.catdogeats.admins.domain.dto.AdminInfo;
import com.team5.catdogeats.admins.util.AdminControllerUtils;
import com.team5.catdogeats.support.domain.notice.dto.NoticeCreateRequestDTO;
import com.team5.catdogeats.support.domain.notice.dto.NoticeResponseDTO;
import com.team5.catdogeats.support.domain.notice.dto.NoticeUpdateRequestDTO;
import com.team5.catdogeats.support.domain.notice.service.NoticeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/v1/admin/notice")  // ← /manage 추가
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminPageController {

    private final NoticeService noticeService;
    private final AdminControllerUtils controllerUtils;

    // ========== 관리자 공지사항 페이지 표시 ==========
    @GetMapping
    public String showNoticeManagePage(HttpSession session, Model model) {
        try {
            // 세션 인증 확인
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 공지사항 페이지 접근 - adminId: {}, adminName: {}",
                    adminInfo.adminId(), adminInfo.name());

            // 모델에 관리자 정보 추가 (필요시)
            model.addAttribute("adminInfo", adminInfo);

            // Thymeleaf 템플릿 반환
            return "thymeleaf/administratorPage_noticeManage"; // 전체 경로

        } catch (Exception e) {
            log.error("관리자 공지사항 페이지 접근 실패", e);
            return "redirect:/v1/admin/login"; // 로그인 페이지로 리다이렉트
        }
    }

    // ========== 공지사항 작성 폼 제출 처리 ==========
    @PostMapping
    public String createNotice(
            HttpSession session,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "draft") String status,
            @RequestParam(defaultValue = "normal") String priority,
            RedirectAttributes redirectAttributes) {

        try {
            // 세션 인증 확인
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 공지사항 생성 요청 - 제목: {}, adminId: {}, adminName: {}",
                    title, adminInfo.adminId(), adminInfo.name());

            // DTO 생성
            NoticeCreateRequestDTO requestDTO = new NoticeCreateRequestDTO();
            requestDTO.setTitle(title);
            requestDTO.setContent(content);

            // 서비스 호출 (REST API와 동일한 서비스 사용)
            NoticeResponseDTO response = noticeService.createNotice(requestDTO);

            log.info("관리자 공지사항 생성 완료 - ID: {}, 제목: {}",
                    response.getId(), response.getTitle());

            // 성공 메시지 추가
            redirectAttributes.addFlashAttribute("successMessage",
                    "공지사항이 성공적으로 등록되었습니다.");
            redirectAttributes.addFlashAttribute("createdNoticeId", response.getId());

            return "redirect:/v1/admin/notice";

        } catch (Exception e) {
            log.error("관리자 공지사항 생성 실패 - 제목: {}", title, e);

            // 에러 메시지 추가
            redirectAttributes.addFlashAttribute("errorMessage",
                    "공지사항 등록에 실패했습니다: " + e.getMessage());

            return "redirect:/v1/admin/notice";
        }
    }

    // ========== 공지사항 수정 폼 제출 처리 ==========
    @PostMapping("/{noticeId}/update")
    public String updateNotice(
            HttpSession session,
            @PathVariable String noticeId,
            @RequestParam String title,
            @RequestParam String content,
            RedirectAttributes redirectAttributes) {

        try {
            // 세션 인증 확인
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 공지사항 수정 요청 - ID: {}, 제목: {}, adminId: {}",
                    noticeId, title, adminInfo.adminId());

            // DTO 생성
            NoticeUpdateRequestDTO requestDTO = new NoticeUpdateRequestDTO();
            requestDTO.setTitle(title);
            requestDTO.setContent(content);

            // 서비스 호출
            NoticeResponseDTO response = noticeService.updateNotice(noticeId, requestDTO);

            log.info("관리자 공지사항 수정 완료 - ID: {}, 제목: {}",
                    response.getId(), response.getTitle());

            redirectAttributes.addFlashAttribute("successMessage",
                    "공지사항이 성공적으로 수정되었습니다.");

            return "redirect:/v1/admin/notice";

        } catch (Exception e) {
            log.error("관리자 공지사항 수정 실패 - ID: {}", noticeId, e);

            redirectAttributes.addFlashAttribute("errorMessage",
                    "공지사항 수정에 실패했습니다: " + e.getMessage());

            return "redirect:/v1/admin/notice";
        }
    }

    // ========== 공지사항 삭제 처리 ==========
    @PostMapping("/{noticeId}/delete")
    public String deleteNotice(
            HttpSession session,
            @PathVariable String noticeId,
            RedirectAttributes redirectAttributes) {

        try {
            // 세션 인증 확인
            AdminInfo adminInfo = controllerUtils.requireSessionInfo(session);

            log.info("관리자 공지사항 삭제 요청 - ID: {}, adminId: {}",
                    noticeId, adminInfo.adminId());

            // 서비스 호출
            noticeService.deleteNotice(noticeId);

            log.info("관리자 공지사항 삭제 완료 - ID: {}", noticeId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "공지사항이 성공적으로 삭제되었습니다.");

            return "redirect:/v1/admin/notice";

        } catch (Exception e) {
            log.error("관리자 공지사항 삭제 실패 - ID: {}", noticeId, e);

            redirectAttributes.addFlashAttribute("errorMessage",
                    "공지사항 삭제에 실패했습니다: " + e.getMessage());

            return "redirect:/v1/admin/notice";
        }
    }
}