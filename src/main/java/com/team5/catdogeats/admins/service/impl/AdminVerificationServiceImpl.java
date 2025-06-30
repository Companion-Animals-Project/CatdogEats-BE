package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminPasswordResetVerificationDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationResponseDTO;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminVerificationService;
import com.team5.catdogeats.admins.service.RedisVerificationCodeService;
import com.team5.catdogeats.admins.util.AdminUtils;
import com.team5.catdogeats.global.config.JpaTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Redis 기반 관리자 계정 인증 서비스 구현체
 * 인증코드를 Redis에서 관리하여 TTL로 자동 만료 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVerificationServiceImpl implements AdminVerificationService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminUtils adminUtils;
    private final RedisVerificationCodeService redisVerificationCodeService;

    @Override
    @JpaTransactional
    public AdminVerificationResponseDTO verifyAdmin(AdminVerificationRequestDTO request) {
        // 1. 이메일로 관리자 조회
        Admins admin = findAdminByEmail(request.email());

        // 2. 이미 활성화되어 있고 첫 로그인도 완료된 계정인지 확인
        if (admin.getIsActive() && !admin.getIsFirstLogin()) {
            return buildAlreadyActiveResponse(admin);
        }

        // 3. Redis에서 인증코드 검증
        if (!redisVerificationCodeService.verifyAndDeleteCode(request.email(), request.verificationCode())) {
            throw new IllegalArgumentException("잘못된 인증코드이거나 만료된 인증코드입니다.");
        }

        // 4. 계정 활성화 및 초기 비밀번호 생성
        String initialPassword = activateAdminAccount(admin);

        log.info("관리자 계정 활성화 완료: email={}, name={}", admin.getEmail(), admin.getName());



        return AdminVerificationResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .isVerified(true)
                .message("계정이 성공적으로 활성화되었습니다. 아래 초기 비밀번호로 로그인해주세요.")
                .redirectUrl(adminUtils.getLoginRedirectUrl())
                .initialPassword(initialPassword)
                .build();
    }

    @Override
    @JpaTransactional
    public String resendVerificationCode(String email) {
        Admins admin = findAdminByEmail(email);

        if (admin.getIsActive()) {
            throw new IllegalStateException("이미 활성화된 계정입니다.");
        }

        // 새로운 인증코드 생성 및 Redis에 저장
        String newCode = adminUtils.generateVerificationCode();
        redisVerificationCodeService.saveVerificationCode(email, newCode);

        // 재발송 이메일 발송
        adminUtils.sendResendVerificationEmail(admin.getEmail(), admin.getName(), newCode);

        log.info("인증코드 재발송: email={}", email);
        return newCode;
    }


    /**
     * 이메일로 관리자 조회
     */
    private Admins findAdminByEmail(String email) {
        return adminRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));
    }

    /**
     * 이미 활성화된 계정에 대한 응답 생성
     */
    private AdminVerificationResponseDTO buildAlreadyActiveResponse(Admins admin) {
        return AdminVerificationResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .isVerified(true)
                .message("이미 활성화된 계정입니다.")
                .redirectUrl(adminUtils.getLoginRedirectUrl())
                .initialPassword(null)
                .build();
    }

    /**
     * 관리자 계정 활성화 및 초기 비밀번호 설정
     */
    private String activateAdminAccount(Admins admin) {
        String initialPassword = adminUtils.generateInitialPassword();

        admin.setIsActive(true);
        admin.setPassword(passwordEncoder.encode(initialPassword));
        admin.setIsFirstLogin(true); // 초기 비밀번호이므로 반드시 변경 필요

        adminRepository.save(admin);

        return initialPassword;
    }

}