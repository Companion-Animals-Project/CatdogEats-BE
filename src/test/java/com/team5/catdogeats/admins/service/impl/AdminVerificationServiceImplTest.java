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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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

        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.of(testAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .willReturn(true);
        given(adminUtils.generateInitialPassword()).willReturn(initialPassword);
        given(passwordEncoder.encode(initialPassword)).willReturn("encodedInitialPassword");
        given(adminUtils.getLoginRedirectUrl()).willReturn(loginRedirectUrl);

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
        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.empty());

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
        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.of(testAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .willReturn(false);

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

        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.of(testAdmin));
        given(adminUtils.getLoginRedirectUrl()).willReturn("http://localhost:8080/v1/admin/login");

        // when
        AdminVerificationResponseDTO response = adminVerificationService.verifyAdmin(verificationRequest);

        // then
        assertThat(response.email()).isEqualTo(testAdmin.getEmail());
        assertThat(response.name()).isEqualTo(testAdmin.getName());
        assertThat(response.isVerified()).isTrue();
        assertThat(response.message()).isEqualTo("이미 활성화된 계정입니다.");
        assertThat(response.redirectUrl()).isEqualTo("http://localhost:8080/v1/admin/login");
        assertThat(response.initialPassword()).isNull();

        // 이미 활성화된 계정이므로 추가 처리가 없어야 함
        verify(redisVerificationCodeService, never()).verifyAndDeleteCode(any(), any());
        verify(adminUtils, never()).generateInitialPassword();
        verify(passwordEncoder, never()).encode(any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성화되었지만 첫 로그인인 계정 인증 시도")
    void verifyAdmin_ActiveButFirstLogin() {
        // given
        testAdmin.setIsActive(true);
        testAdmin.setIsFirstLogin(true); // 여전히 첫 로그인

        String initialPassword = "newInitialPass123";
        String loginRedirectUrl = "http://localhost:8080/v1/admin/login";

        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.of(testAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .willReturn(true);
        given(adminUtils.generateInitialPassword()).willReturn(initialPassword);
        given(passwordEncoder.encode(initialPassword)).willReturn("encodedNewInitialPassword");
        given(adminUtils.getLoginRedirectUrl()).willReturn(loginRedirectUrl);

        // when
        AdminVerificationResponseDTO response = adminVerificationService.verifyAdmin(verificationRequest);

        // then
        assertThat(response.email()).isEqualTo(testAdmin.getEmail());
        assertThat(response.name()).isEqualTo(testAdmin.getName());
        assertThat(response.isVerified()).isTrue();
        assertThat(response.message()).isEqualTo("계정이 성공적으로 활성화되었습니다. 아래 초기 비밀번호로 로그인해주세요.");
        assertThat(response.redirectUrl()).isEqualTo(loginRedirectUrl);
        assertThat(response.initialPassword()).isEqualTo(initialPassword);

        // Redis 인증코드 검증 및 계정 처리 확인
        verify(redisVerificationCodeService).verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode());
        verify(adminRepository).save(testAdmin);
    }

    @Test
    @DisplayName("인증코드 재발송 성공")
    void resendVerificationCode_Success() {
        // given
        String email = "test@example.com";
        String newCode = "654321";

        given(adminRepository.findByEmail(email)).willReturn(Optional.of(testAdmin));
        given(adminUtils.generateVerificationCode()).willReturn(newCode);

        // when
        String result = adminVerificationService.resendVerificationCode(email);

        // then
        assertThat(result).isEqualTo(newCode);

        // 새로운 인증코드 생성 및 Redis 저장 확인
        verify(adminUtils).generateVerificationCode();
        verify(redisVerificationCodeService).saveVerificationCode(email, newCode);

        // 재발송 이메일 발송 확인
        verify(adminUtils).sendResendVerificationEmail(testAdmin.getEmail(), testAdmin.getName(), newCode);
    }

    @Test
    @DisplayName("인증코드 재발송 실패 - 존재하지 않는 이메일")
    void resendVerificationCode_Fail_EmailNotFound() {
        // given
        String email = "nonexistent@example.com";
        given(adminRepository.findByEmail(email)).willReturn(Optional.empty());

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
        String email = "test@example.com";
        testAdmin.setIsActive(true);

        given(adminRepository.findByEmail(email)).willReturn(Optional.of(testAdmin));

        // when & then
        assertThatThrownBy(() -> adminVerificationService.resendVerificationCode(email))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 활성화된 계정입니다.");

        verify(adminUtils, never()).generateVerificationCode();
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminUtils, never()).sendResendVerificationEmail(any(), any(), any());
    }

    @Test
    @DisplayName("계정 활성화 로직 검증")
    void activateAdminAccount_Logic() {
        // given
        String initialPassword = "generatedPassword123";

        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.of(testAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .willReturn(true);
        given(adminUtils.generateInitialPassword()).willReturn(initialPassword);
        given(passwordEncoder.encode(initialPassword)).willReturn("encodedGeneratedPassword");
        given(adminUtils.getLoginRedirectUrl()).willReturn("http://localhost:8080/v1/admin/login");

        // when
        adminVerificationService.verifyAdmin(verificationRequest);

        // then
        // 계정 상태 변경 확인
        assertThat(testAdmin.getIsActive()).isTrue();
        assertThat(testAdmin.getPassword()).isEqualTo("encodedGeneratedPassword");
        assertThat(testAdmin.getIsFirstLogin()).isTrue(); // 초기 비밀번호이므로 반드시 변경 필요

        // 초기 비밀번호 생성 및 암호화 확인
        verify(adminUtils).generateInitialPassword();
        verify(passwordEncoder).encode(initialPassword);
    }

    @Test
    @DisplayName("Redis 인증코드 처리 검증")
    void redisVerificationCodeHandling() {
        // given
        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.of(testAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .willReturn(true);
        given(adminUtils.generateInitialPassword()).willReturn("initialPass123");
        given(passwordEncoder.encode("initialPass123")).willReturn("encodedInitialPassword");
        given(adminUtils.getLoginRedirectUrl()).willReturn("http://localhost:8080/v1/admin/login");

        // when
        adminVerificationService.verifyAdmin(verificationRequest);

        // then
        // Redis에서 인증코드 검증 및 삭제가 올바르게 호출되었는지 확인
        verify(redisVerificationCodeService).verifyAndDeleteCode(
                verificationRequest.email(),
                verificationRequest.verificationCode()
        );
    }

    @Test
    @DisplayName("이메일 발송 검증 - 재발송")
    void emailSending_Resend() {
        // given
        String email = "test@example.com";
        String newCode = "999888";

        given(adminRepository.findByEmail(email)).willReturn(Optional.of(testAdmin));
        given(adminUtils.generateVerificationCode()).willReturn(newCode);

        // when
        adminVerificationService.resendVerificationCode(email);

        // then
        // 정확한 파라미터로 재발송 이메일 메서드가 호출되었는지 확인
        verify(adminUtils).sendResendVerificationEmail(
                testAdmin.getEmail(),
                testAdmin.getName(),
                newCode
        );
    }

    @Test
    @DisplayName("다양한 부서 관리자 인증 처리")
    void verifyAdmin_DifferentDepartments() {
        // given
        Admins csAdmin = Admins.builder()
                .id("admin-2")
                .email("cs@example.com")
                .name("고객서비스 관리자")
                .password("encodedTempPassword")
                .department(Department.CUSTOMER_SERVICE)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(false)
                .isFirstLogin(true)
                .build();

        AdminVerificationRequestDTO csRequest = new AdminVerificationRequestDTO("cs@example.com", "123456");
        String initialPassword = "csInitialPass123";

        given(adminRepository.findByEmail(csRequest.email())).willReturn(Optional.of(csAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(csRequest.email(), csRequest.verificationCode()))
                .willReturn(true);
        given(adminUtils.generateInitialPassword()).willReturn(initialPassword);
        given(passwordEncoder.encode(initialPassword)).willReturn("encodedCSInitialPassword");
        given(adminUtils.getLoginRedirectUrl()).willReturn("http://localhost:8080/v1/admin/login");

        // when
        AdminVerificationResponseDTO response = adminVerificationService.verifyAdmin(csRequest);

        // then
        assertThat(response.email()).isEqualTo("cs@example.com");
        assertThat(response.name()).isEqualTo("고객서비스 관리자");
        assertThat(response.isVerified()).isTrue();
        assertThat(response.initialPassword()).isEqualTo(initialPassword);

        // 부서는 변경되지 않음을 확인
        assertThat(csAdmin.getDepartment()).isEqualTo(Department.CUSTOMER_SERVICE);
        assertThat(csAdmin.getIsActive()).isTrue();
    }
}