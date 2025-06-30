package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminPasswordResetRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminPasswordResetResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminPasswordResetVerificationDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationResponseDTO;
import com.team5.catdogeats.admins.domain.enums.AdminRole;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.RedisVerificationCodeService;
import com.team5.catdogeats.admins.util.AdminUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 비밀번호 초기화 서비스 테스트")
class AdminPasswordResetServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminUtils adminUtils;

    @Mock
    private RedisVerificationCodeService redisVerificationCodeService;

    @InjectMocks
    private AdminPasswordResetServiceImpl adminPasswordResetService;

    private Admins targetAdmin;
    private AdminPasswordResetRequestDTO resetRequest;
    private ZonedDateTime mockExpiryTime;

    @BeforeEach
    void setUp() {
        // 슈퍼관리자 이메일 설정
        ReflectionTestUtils.setField(adminPasswordResetService, "superAdminEmail", "super@admin.com");

        targetAdmin = Admins.builder()
                .id("admin-1")
                .email("target@example.com")
                .name("대상 관리자")
                .password("encodedOldPassword")
                .department(Department.DEVELOPMENT)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(true)
                .isFirstLogin(false)
                .build();

        resetRequest = new AdminPasswordResetRequestDTO(
                "target@example.com",
                "requester@admin.com"
        );

        mockExpiryTime = ZonedDateTime.now().plusHours(1);
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 성공")
    void requestPasswordReset_Success() {
        // given
        String verificationCode = "123456";

        when(adminRepository.findByEmail(resetRequest.email())).thenReturn(Optional.of(targetAdmin));
        when(adminUtils.generateVerificationCode()).thenReturn(verificationCode);
        when(redisVerificationCodeService.calculateExpiryTime()).thenReturn(mockExpiryTime);

        // when
        AdminPasswordResetResponseDTO response = adminPasswordResetService.requestPasswordReset(resetRequest);

        // then
        assertThat(response.email()).isEqualTo(targetAdmin.getEmail());
        assertThat(response.name()).isEqualTo(targetAdmin.getName());
        assertThat(response.verificationCodeExpiry()).isEqualTo(mockExpiryTime);
        assertThat(response.message()).isEqualTo("비밀번호 초기화 이메일이 발송되었습니다. 사용자가 인증 후 초기 비밀번호를 받아 변경할 수 있습니다.");

        // Redis에 인증코드 저장 검증
        verify(redisVerificationCodeService).saveVerificationCode(targetAdmin.getEmail(), verificationCode);

        // 계정 비활성화 검증
        ArgumentCaptor<Admins> adminCaptor = ArgumentCaptor.forClass(Admins.class);
        verify(adminRepository).save(adminCaptor.capture());
        Admins savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getIsActive()).isFalse();
        assertThat(savedAdmin.getIsFirstLogin()).isTrue();

        // 비밀번호 초기화 이메일 발송 검증
        verify(adminUtils).sendPasswordResetEmail(
                targetAdmin.getEmail(),
                targetAdmin.getName(),
                verificationCode,
                resetRequest.requestedBy()
        );
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 실패 - 존재하지 않는 관리자")
    void requestPasswordReset_Fail_AdminNotFound() {
        // given
        when(adminRepository.findByEmail(resetRequest.email())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.requestPasswordReset(resetRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 관리자입니다.");

        verify(adminUtils, never()).generateVerificationCode();
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminUtils, never()).sendPasswordResetEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 실패 - 슈퍼관리자 계정")
    void requestPasswordReset_Fail_SuperAdminAccount() {
        // given
        Admins superAdmin = Admins.builder()
                .email("super@admin.com")
                .name("슈퍼관리자")
                .build();

        AdminPasswordResetRequestDTO superAdminResetRequest = new AdminPasswordResetRequestDTO(
                "super@admin.com",
                "requester@admin.com"
        );

        when(adminRepository.findByEmail("super@admin.com")).thenReturn(Optional.of(superAdmin));

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.requestPasswordReset(superAdminResetRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("슈퍼관리자 계정은 비밀번호를 초기화할 수 없습니다.");

        verify(adminUtils, never()).generateVerificationCode();
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminUtils, never()).sendPasswordResetEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 실패 - 자기 자신 초기화 방지")
    void requestPasswordReset_Fail_SelfReset() {
        // given
        AdminPasswordResetRequestDTO selfResetRequest = new AdminPasswordResetRequestDTO(
                "target@example.com",
                "target@example.com" // 같은 이메일
        );

        when(adminRepository.findByEmail(selfResetRequest.email())).thenReturn(Optional.of(targetAdmin));

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.requestPasswordReset(selfResetRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자신의 비밀번호는 초기화할 수 없습니다. 비밀번호 변경 기능을 이용하세요.");

        verify(adminUtils, never()).generateVerificationCode();
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminUtils, never()).sendPasswordResetEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("비밀번호 재설정 검증 성공")
    void verifyAndResetPassword_Success() {
        // given
        AdminPasswordResetVerificationDTO verificationRequest = new AdminPasswordResetVerificationDTO(
                "target@example.com",
                "123456",
                "newPassword123",
                "newPassword123"
        );

        String loginRedirectUrl = "http://localhost:8080/v1/admin/login";

        when(adminRepository.findByEmail(verificationRequest.email())).thenReturn(Optional.of(targetAdmin));
        when(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .thenReturn(true);
        when(passwordEncoder.encode(verificationRequest.newPassword())).thenReturn("encodedNewPassword");
        when(adminUtils.getLoginRedirectUrl()).thenReturn(loginRedirectUrl);

        // when
        AdminVerificationResponseDTO response = adminPasswordResetService.verifyAndResetPassword(verificationRequest);

        // then
        assertThat(response.email()).isEqualTo(targetAdmin.getEmail());
        assertThat(response.name()).isEqualTo(targetAdmin.getName());
        assertThat(response.isVerified()).isTrue();
        assertThat(response.message()).isEqualTo("비밀번호가 성공적으로 재설정되었습니다. 새 비밀번호로 로그인해주세요.");
        assertThat(response.redirectUrl()).isEqualTo(loginRedirectUrl);
        assertThat(response.initialPassword()).isNull(); // 보안상 새 비밀번호는 반환하지 않음

        // 비밀번호 변경 및 계정 활성화 검증
        ArgumentCaptor<Admins> adminCaptor = ArgumentCaptor.forClass(Admins.class);
        verify(adminRepository).save(adminCaptor.capture());
        Admins savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getPassword()).isEqualTo("encodedNewPassword");
        assertThat(savedAdmin.getIsActive()).isTrue();
        assertThat(savedAdmin.getIsFirstLogin()).isFalse(); // 비밀번호 재설정 완료로 간주

        // Redis 인증코드 검증 및 삭제 확인
        verify(redisVerificationCodeService).verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode());
    }

    @Test
    @DisplayName("비밀번호 재설정 검증 실패 - 존재하지 않는 이메일")
    void verifyAndResetPassword_Fail_EmailNotFound() {
        // given
        AdminPasswordResetVerificationDTO verificationRequest = new AdminPasswordResetVerificationDTO(
                "nonexistent@example.com",
                "123456",
                "newPassword123",
                "newPassword123"
        );

        when(adminRepository.findByEmail(verificationRequest.email())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.verifyAndResetPassword(verificationRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 이메일입니다.");

        verify(redisVerificationCodeService, never()).verifyAndDeleteCode(any(), any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 재설정 검증 실패 - 잘못된 인증코드")
    void verifyAndResetPassword_Fail_InvalidVerificationCode() {
        // given
        AdminPasswordResetVerificationDTO verificationRequest = new AdminPasswordResetVerificationDTO(
                "target@example.com",
                "wrongCode",
                "newPassword123",
                "newPassword123"
        );

        when(adminRepository.findByEmail(verificationRequest.email())).thenReturn(Optional.of(targetAdmin));
        when(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.verifyAndResetPassword(verificationRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잘못된 인증코드이거나 만료된 인증코드입니다.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 재설정 검증 실패 - 새 비밀번호 불일치")
    void verifyAndResetPassword_Fail_PasswordMismatch() {
        // given
        AdminPasswordResetVerificationDTO verificationRequest = new AdminPasswordResetVerificationDTO(
                "target@example.com",
                "123456",
                "newPassword123",
                "differentPassword" // 다른 비밀번호
        );

        when(adminRepository.findByEmail(verificationRequest.email())).thenReturn(Optional.of(targetAdmin));
        when(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.verifyAndResetPassword(verificationRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");

        verify(passwordEncoder, never()).encode(any());
        verify(adminRepository, never()).save(any());
    }
}