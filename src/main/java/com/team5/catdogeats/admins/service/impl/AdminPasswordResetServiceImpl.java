package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminPasswordResetService;
import com.team5.catdogeats.admins.service.RedisVerificationCodeService;
import com.team5.catdogeats.admins.util.AdminUtils;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 관리자 비밀번호 초기화 서비스
 * 인증코드를 Redis에서 관리하여 TTL로 자동 만료 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPasswordResetServiceImpl implements AdminPasswordResetService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminUtils adminUtils;
    private final RedisVerificationCodeService redisVerificationCodeService;

    @Value("${admin.super.email}")
    private String superAdminEmail;

    @Override
    @JpaTransactional
    public AdminPasswordResetResponseDTO requestPasswordReset(AdminPasswordResetRequestDTO request) {
        // 1. 대상 관리자 조회 및 검증
        Admins targetAdmin = findAndValidateTargetAdmin(request);

        // 2. 인증코드 생성 및 Redis에 저장
        String verificationCode = adminUtils.generateVerificationCode();
        redisVerificationCodeService.saveVerificationCode(targetAdmin.getEmail(), verificationCode);

        // 3. 계정 비활성화
        resetAdminAccount(targetAdmin);

        // 4. 비밀번호 초기화 이메일 발송
        adminUtils.sendPasswordResetEmail(
                targetAdmin.getEmail(),
                targetAdmin.getName(),
                verificationCode,
                request.requestedBy()
        );

        log.info("비밀번호 초기화 요청 완료: target={}, requestedBy={}",
                targetAdmin.getEmail(), request.requestedBy());

        return AdminPasswordResetResponseDTO.builder()
                .email(targetAdmin.getEmail())
                .name(targetAdmin.getName())
                .verificationCodeExpiry(redisVerificationCodeService.calculateExpiryTime())
                .message("비밀번호 초기화 이메일이 발송되었습니다. 사용자가 인증 후 초기 비밀번호를 받아 변경할 수 있습니다.")
                .build();
    }

    @Override
    @JpaTransactional
    public AdminVerificationResponseDTO verifyAndResetPassword(AdminPasswordResetVerificationDTO request) {
        // 1. 관리자 조회
        Admins admin = findAdminByEmail(request.email());

        // 2. Redis에서 비밀번호 재설정 인증코드 검증 및 삭제
        if (!redisVerificationCodeService.verifyAndDeleteCode(request.email(), request.verificationCode())) {
            throw new IllegalArgumentException("잘못된 인증코드이거나 만료된 인증코드입니다.");
        }

        // 3. 새 비밀번호 검증
        validatePasswordMatch(request.newPassword(), request.confirmPassword());

        // 4. 비밀번호 변경 및 계정 활성화
        resetPassword(admin, request.newPassword());

        log.info("비밀번호 초기화 완료: email={}", admin.getEmail());

        return AdminVerificationResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .isVerified(true)
                .message("비밀번호가 성공적으로 재설정되었습니다. 새 비밀번호로 로그인해주세요.")
                .redirectUrl(adminUtils.getLoginRedirectUrl())
                .initialPassword(null) // 보안상 새 비밀번호는 반환하지 않음
                .build();
    }

    /**
     * 대상 관리자 조회 및 유효성 검증
     */
    private Admins findAndValidateTargetAdmin(AdminPasswordResetRequestDTO request) {
        Admins targetAdmin = adminRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        // 슈퍼관리자 계정은 초기화 불가
        if (superAdminEmail.equals(targetAdmin.getEmail())) {
            throw new IllegalArgumentException("슈퍼관리자 계정은 비밀번호를 초기화할 수 없습니다.");
        }

        // 자기 자신 초기화 방지
        if (request.email().equals(request.requestedBy())) {
            throw new IllegalArgumentException("자신의 비밀번호는 초기화할 수 없습니다. 비밀번호 변경 기능을 이용하세요.");
        }

        return targetAdmin;
    }

    /**
     * 이메일로 관리자 조회
     */
    private Admins findAdminByEmail(String email) {
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
    }

    /**
     * 관리자 계정 초기화 (비활성화만, 비밀번호는 인증 후 새로 생성)
     */
    private void resetAdminAccount(Admins admin) {
        admin.setIsActive(false);    // 비활성화
        admin.setIsFirstLogin(true); // 첫 로그인 상태로 되돌림
        // 비밀번호는 그대로 두고, 인증 후에 새로 생성
        adminRepository.save(admin);
    }

    /**
     * 비밀번호 일치 검증
     */
    private void validatePasswordMatch(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }
    }

    /**
     * 비밀번호 재설정 및 계정 활성화
     */
    private void resetPassword(Admins admin, String newPassword) {
        admin.setPassword(passwordEncoder.encode(newPassword));
        admin.setIsActive(true);
        admin.setIsFirstLogin(false); // 비밀번호 재설정 완료로 간주
        adminRepository.save(admin);
    }
}