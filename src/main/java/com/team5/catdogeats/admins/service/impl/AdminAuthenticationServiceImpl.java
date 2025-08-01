package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.repository.AdminSessionRepository;
import com.team5.catdogeats.admins.service.AdminAuthenticationService;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 관리자 인증 서비스 구현체
 * Spring Security 세션 기반 로그인 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthenticationServiceImpl implements AdminAuthenticationService {

    private final AdminRepository adminRepository;
    private final AdminSessionRepository adminSessionRepository;
    private final PasswordEncoder passwordEncoder;


    @Value("${admin.session.key:ADMIN_USER}")
    private String adminSessionKey;

    @Override
    @JpaTransactional
    public AdminLoginResponseDTO login(AdminLoginRequestDTO request, HttpSession session) {
        // 1. 이메일로 관리자 조회
        Admins admin = adminRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 2. 계정 활성화 상태 확인
        if (!admin.getIsActive()) {
            throw new IllegalStateException("계정이 활성화되지 않았습니다. 이메일을 확인해주세요.");
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 4. 첫 로그인 상태 저장
        boolean isFirstLogin = admin.getIsFirstLogin();

        // 5. 로그인 시간 업데이트
        admin.updateLastLoginAt();
        adminRepository.save(admin);

        // 6. Spring Security Authentication 객체 생성 및 설정
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        authorities.add(new SimpleGrantedAuthority(admin.getDepartment().name()));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                admin.getEmail(),
                null,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 7. 세션에 관리자 정보 저장
        AdminSession adminSession = AdminSession.builder()
                .sessionId(session.getId())
                .adminId(admin.getId())
                .build();

        adminSessionRepository.save(adminSession);

        // 8. 리다이렉트 URL 결정 - 첫 로그인이면 비밀번호 변경 권장
        String redirectUrl = isFirstLogin ? "/v1/admin/change-password" : "/v1/admin/dashboard";

        String message = isFirstLogin ?
                "첫 로그인입니다. 보안을 위해 비밀번호를 변경해주세요." :
                "로그인 성공";

        log.info("관리자 로그인 성공: email={}, name={}, department={}, isFirstLogin={}",
                admin.getEmail(), admin.getName(), admin.getDepartment(), isFirstLogin);

        return AdminLoginResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .department(admin.getDepartment())
                .isFirstLogin(isFirstLogin)
                .lastLoginAt(admin.getLastLoginAt())
                .redirectUrl(redirectUrl)
                .message(message)
                .build();
    }

    @Override
    public void logout(HttpSession session) {
        // Redis에서 세션 삭제
        adminSessionRepository.deleteById(session.getId());

        // Spring Security 컨텍스트 클리어
        SecurityContextHolder.clearContext();
        session.invalidate();

        log.info("관리자 로그아웃 완료: sessionId={}", session.getId());
    }

    @Override
    @JpaTransactional
    public void changePassword(AdminPasswordChangeRequestDTO request, HttpSession session) {
        // adminId 조회
        AdminSession adminSession = adminSessionRepository.findById(session.getId())
                .orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."));


        Admins admin = adminRepository.findById(adminSession.getAdminId())
                .orElseThrow(() -> new IllegalStateException("관리자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(request.currentPassword(), admin.getPassword())) {
            throw new BadCredentialsException("현재 비밀번호가 올바르지 않습니다.");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }

        admin.changePassword(passwordEncoder.encode(request.newPassword()));
        admin.completeFirstLogin();
        adminRepository.save(admin);

        log.info("관리자 비밀번호 변경 완료: email={}", admin.getEmail());
    }

    @Override
    public AdminInfo getSessionInfo(HttpSession session) {
        // 1. Redis에서 adminId만 조회
        AdminSession adminSession = adminSessionRepository.findById(session.getId())
                .orElse(null);

        if (adminSession == null || !adminSession.isValid()) {
            return null;
        }


        Admins admin = adminRepository.findById(adminSession.getAdminId())
                .orElse(null);

        if (admin == null) {
            // adminId가 유효하지 않으면 세션 삭제
            adminSessionRepository.deleteById(session.getId());
            return null;
        }

        return new AdminInfo(
                admin.getId(),
                admin.getEmail(),
                admin.getName(),
                admin.getDepartment(),
                admin.getIsFirstLogin(),
                admin.getLastLoginAt().withZoneSameInstant(ZoneId.of("Asia/Seoul"))
        );
    }

    @Override
    public boolean isLoggedIn(HttpSession session) {
        // 1. Spring Security 인증 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSecurityAuthenticated = auth != null &&
                auth.isAuthenticated() &&
                !"anonymousUser".equals(auth.getPrincipal());

        // 2. Redis 세션 확인 (adminId만 확인)
        boolean hasValidSession = adminSessionRepository.findById(session.getId())
                .map(AdminSession::isValid)
                .orElse(false);

        return isSecurityAuthenticated && hasValidSession;
    }
}