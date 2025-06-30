package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationRequestDTO;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 계정 인증 서비스 테스트")
class AdminVerificationServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminUtils adminUtils;

    @Mock
    private RedisVerificationCodeService redisVerificationCodeService;

    @InjectMocks
    private AdminVerificationServiceImpl adminVerificationService;

    private Admins testAdmin;
    private AdminVerificationRequestDTO verificationRequest;

    @BeforeEach
    void setUp() {
        testAdmin = Admins.builder()
                .id("admin-1")
                .email("test@example.com")
                .name("테스트 사용자")
                .password("encodedTempPassword")
                .department(Department.DEVELOPMENT)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(false)  // 비활성 상태
                .isFirstLogin(true)
                .build();

        verificationRequest = new AdminVerificationRequestDTO("test@example.com", "123456");
    }

    @Test
    @DisplayName("계정 인증 성공")
    void verifyAdmin_Success() {
        // given
        String initialPassword = "initialPass123";
        String loginRedirectUrl = "http://localhost:8080/v1/admin/login";

        when(adminRepository.findByEmail(verificationRequest.email())).thenReturn(Optional.of(testAdmin));
        when(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .thenReturn(true);
        when(adminUtils.generateInitialPassword()).thenReturn(initialPassword);
        when(passwordEncoder.encode(initialPassword)).thenReturn("encodedInitialPassword");
        when(adminUtils.getLoginRedirectUrl()).thenReturn(loginRedirectUrl);

        // when
        AdminVerificationResponseDTO response = adminVerificationService.verifyAdmin(verificationRequest);

        // then
        assertThat(response.email()).isEqualTo(testAdmin.getEmail());
        assertThat(response.name()).isEqualTo(testAdmin.getName());
        assertThat(response.isVerified()).isTrue();
        assertThat(response.message()).isEqualTo("계정이 성공적으로 활성화되었습니다. 아래 초기 비밀번호로 로그인해주세요.");
        assertThat(response.redirectUrl()).isEqualTo(loginRedirectUrl);
        assertThat(response.initialPassword()).isEqualTo(initialPassword);

        // 계정 활성화 검증
        verify(adminRepository).save(testAdmin);
        assertThat(testAdmin.getIsActive()).isTrue();
        assertThat(testAdmin.getPassword()).isEqualTo("encodedInitialPassword");
        assertThat(testAdmin.getIsFirstLogin()).isTrue(); // 여전히 첫 로그인 상태 (비밀번호 변경 필요)

        // Redis 인증코드 검증 및 삭제 확인
        verify(redisVerificationCodeService).verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode());
    }

    @Test
    @DisplayName("계정 인증 실패 - 존재하지 않는 이메일")
    void verifyAdmin_Fail_EmailNotFound() {
        // given
        when(adminRepository.findByEmail(verificationRequest.email())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminVerificationService.verifyAdmin(verificationRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 이메일입니다.");

        verify(redisVerificationCodeService, never()).verifyAndDeleteCode(any(), any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("계정 인증 실패 - 잘못된 인증코드")
    void verifyAdmin_Fail_InvalidVerificationCode() {
        // given
        when(adminRepository.findByEmail(verificationRequest.email())).thenReturn(Optional.of(testAdmin));
        when(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> adminVerificationService.verifyAdmin(verificationRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잘못된 인증코드이거나 만료된 인증코드입니다.");

        verify(adminUtils, never()).generateInitialPassword();
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 활성화된 계정 인증 시도")
    void verifyAdmin_AlreadyActive() {
        // given
        testAdmin.setIsActive(true);
        testAdmin.setIsFirstLogin(false);

        when(adminRepository.findByEmail(verificationRequest.email())).thenReturn(Optional.of(testAdmin));
        when(adminUtils.getLoginRedirectUrl()).thenReturn("http://localhost:8080/v1/admin/login");

        // when
        AdminVerificationResponseDTO response = adminVerificationService.verifyAdmin(verificationRequest);

        // then
        assertThat(response.email()).isEqualTo(testAdmin.getEmail());
        assertThat(response.name()).isEqualTo(testAdmin.getName());
        assertThat(response.isVerified()).isTrue();
        assertThat(response.message()).isEqualTo("이미 활성화된 계정입니다.");
        assertThat(response.initialPassword()).isNull();

        // 이미 활성화된 계정이므로 추가 처리는 하지 않음
        verify(redisVerificationCodeService, never()).verifyAndDeleteCode(any(), any());
        verify(adminUtils, never()).generateInitialPassword();
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("인증코드 재발송 성공")
    void resendVerificationCode_Success() {
        // given
        String newCode = "654321";
        when(adminRepository.findByEmail(testAdmin.getEmail())).thenReturn(Optional.of(testAdmin));
        when(adminUtils.generateVerificationCode()).thenReturn(newCode);

        // when
        String result = adminVerificationService.resendVerificationCode(testAdmin.getEmail());

        // then
        assertThat(result).isEqualTo(newCode);

        verify(redisVerificationCodeService).saveVerificationCode(testAdmin.getEmail(), newCode);
        verify(adminUtils).sendResendVerificationEmail(testAdmin.getEmail(), testAdmin.getName(), newCode);
    }

    @Test
    @DisplayName("인증코드 재발송 실패 - 존재하지 않는 이메일")
    void resendVerificationCode_Fail_EmailNotFound() {
        // given
        String email = "nonexistent@example.com";
        when(adminRepository.findByEmail(email)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminVerificationService.resendVerificationCode(email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 이메일입니다.");

        verify(adminUtils, never()).generateVerificationCode();
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminUtils, never()).sendResendVerificationEmail(any(), any(), any());
    }

    @Test
    @DisplayName("인증코드 재발송 실패 - 이미 활성화된 계정")
    void resendVerificationCode_Fail_AlreadyActive() {
        // given
        testAdmin.setIsActive(true);
        when(adminRepository.findByEmail(testAdmin.getEmail())).thenReturn(Optional.of(testAdmin));

        // when & then
        assertThatThrownBy(() -> adminVerificationService.resendVerificationCode(testAdmin.getEmail()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 활성화된 계정입니다.");

        verify(adminUtils, never()).generateVerificationCode();
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminUtils, never()).sendResendVerificationEmail(any(), any(), any());
    }

    @Test
    @DisplayName("계정 활성화 과정에서 초기 비밀번호 생성 및 암호화 검증")
    void verifyAdmin_PasswordHandling() {
        // given
        String initialPassword = "TempPass123";
        String encodedPassword = "encodedTempPass123";

        when(adminRepository.findByEmail(verificationRequest.email())).thenReturn(Optional.of(testAdmin));
        when(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .thenReturn(true);
        when(adminUtils.generateInitialPassword()).thenReturn(initialPassword);
        when(passwordEncoder.encode(initialPassword)).thenReturn(encodedPassword);
        when(adminUtils.getLoginRedirectUrl()).thenReturn("http://localhost:8080/v1/admin/login");

        ArgumentCaptor<Admins> adminCaptor = ArgumentCaptor.forClass(Admins.class);

        // when
        AdminVerificationResponseDTO response = adminVerificationService.verifyAdmin(verificationRequest);

        // then
        verify(adminRepository).save(adminCaptor.capture());
        Admins savedAdmin = adminCaptor.getValue();

        assertThat(savedAdmin.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedAdmin.getIsActive()).isTrue();
        assertThat(savedAdmin.getIsFirstLogin()).isTrue(); // 초기 비밀번호이므로 변경 필요
        assertThat(response.initialPassword()).isEqualTo(initialPassword); // 응답에는 평문 비밀번호 포함
    }
}