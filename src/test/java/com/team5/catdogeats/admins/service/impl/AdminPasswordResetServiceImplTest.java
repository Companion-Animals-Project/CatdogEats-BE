package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.*;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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

        given(adminRepository.findByEmail(resetRequest.email())).willReturn(Optional.of(targetAdmin));
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        // when
        AdminPasswordResetResponseDTO response = adminPasswordResetService.requestPasswordReset(resetRequest);

        // then
        assertThat(response.email()).isEqualTo(targetAdmin.getEmail());
        assertThat(response.name()).isEqualTo(targetAdmin.getName());
        assertThat(response.verificationCodeExpiry()).isEqualTo(mockExpiryTime);
        assertThat(response.message()).isEqualTo("비밀번호 초기화 이메일이 발송되었습니다. 사용자가 인증 후 초기 비밀번호를 받아 변경할 수 있습니다.");

        // 계정 상태 변경 검증
        assertThat(targetAdmin.getIsActive()).isFalse(); // 비활성화
        assertThat(targetAdmin.getIsFirstLogin()).isTrue(); // 첫 로그인 상태로 되돌림

        // Redis에 인증코드 저장 검증
        verify(redisVerificationCodeService).saveVerificationCode(targetAdmin.getEmail(), verificationCode);

        // 이메일 발송 검증
        verify(adminUtils).sendPasswordResetEmail(
                targetAdmin.getEmail(),
                targetAdmin.getName(),
                verificationCode,
                resetRequest.requestedBy()
        );

        // 관리자 정보 저장 검증
        verify(adminRepository).save(targetAdmin);
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 실패 - 존재하지 않는 관리자")
    void requestPasswordReset_Fail_AdminNotFound() {
        // given
        given(adminRepository.findByEmail(resetRequest.email())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.requestPasswordReset(resetRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 관리자입니다.");

        // 다른 메서드들이 호출되지 않았음을 확인
        verify(adminUtils, never()).generateVerificationCode();
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminUtils, never()).sendPasswordResetEmail(any(), any(), any(), any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 실패 - 슈퍼관리자 계정")
    void requestPasswordReset_Fail_SuperAdminProtection() {
        // given
        AdminPasswordResetRequestDTO superAdminRequest = new AdminPasswordResetRequestDTO(
                "super@admin.com",
                "requester@admin.com"
        );

        Admins superAdmin = Admins.builder()
                .email("super@admin.com")
                .name("슈퍼관리자")
                .build();

        given(adminRepository.findByEmail("super@admin.com")).willReturn(Optional.of(superAdmin));

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.requestPasswordReset(superAdminRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("슈퍼관리자 계정은 비밀번호를 초기화할 수 없습니다.");

        // 다른 메서드들이 호출되지 않았음을 확인
        verify(adminUtils, never()).generateVerificationCode();
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminUtils, never()).sendPasswordResetEmail(any(), any(), any(), any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 실패 - 자기 자신 초기화 시도")
    void requestPasswordReset_Fail_SelfReset() {
        // given
        AdminPasswordResetRequestDTO selfResetRequest = new AdminPasswordResetRequestDTO(
                "target@example.com",
                "target@example.com" // 자기 자신
        );

        given(adminRepository.findByEmail(selfResetRequest.email())).willReturn(Optional.of(targetAdmin));

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.requestPasswordReset(selfResetRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자신의 비밀번호는 초기화할 수 없습니다. 비밀번호 변경 기능을 이용하세요.");

        // 다른 메서드들이 호출되지 않았음을 확인
        verify(adminUtils, never()).generateVerificationCode();
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminUtils, never()).sendPasswordResetEmail(any(), any(), any(), any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 재설정 인증 성공")
    void verifyAndResetPassword_Success() {
        // given
        String newPassword = "newPassword123";
        String confirmPassword = "newPassword123";
        String loginRedirectUrl = "http://localhost:8080/v1/admin/login";

        AdminPasswordResetVerificationDTO verificationRequest = new AdminPasswordResetVerificationDTO(
                targetAdmin.getEmail(),
                "123456",
                newPassword,
                confirmPassword
        );

        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.of(targetAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .willReturn(true);
        given(passwordEncoder.encode(newPassword)).willReturn("encodedNewPassword");
        given(adminUtils.getLoginRedirectUrl()).willReturn(loginRedirectUrl);

        // when
        AdminVerificationResponseDTO response = adminPasswordResetService.verifyAndResetPassword(verificationRequest);

        // then
        assertThat(response.email()).isEqualTo(targetAdmin.getEmail());
        assertThat(response.name()).isEqualTo(targetAdmin.getName());
        assertThat(response.isVerified()).isTrue();
        assertThat(response.message()).isEqualTo("비밀번호가 성공적으로 재설정되었습니다. 새 비밀번호로 로그인해주세요.");
        assertThat(response.redirectUrl()).isEqualTo(loginRedirectUrl);
        assertThat(response.initialPassword()).isNull(); // 보안상 새 비밀번호는 반환하지 않음

        // 계정 상태 확인
        assertThat(targetAdmin.getPassword()).isEqualTo("encodedNewPassword");
        assertThat(targetAdmin.getIsActive()).isTrue(); // 활성화
        assertThat(targetAdmin.getIsFirstLogin()).isFalse(); // 비밀번호 재설정 완료로 간주

        // Redis 인증코드 검증 및 삭제 확인
        verify(redisVerificationCodeService).verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode());

        // 관리자 정보 저장 확인
        verify(adminRepository).save(targetAdmin);
    }

    @Test
    @DisplayName("비밀번호 재설정 인증 실패 - 존재하지 않는 이메일")
    void verifyAndResetPassword_Fail_EmailNotFound() {
        // given
        AdminPasswordResetVerificationDTO verificationRequest = new AdminPasswordResetVerificationDTO(
                "nonexistent@example.com",
                "123456",
                "newPassword123",
                "newPassword123"
        );

        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.verifyAndResetPassword(verificationRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 이메일입니다.");

        verify(redisVerificationCodeService, never()).verifyAndDeleteCode(any(), any());
        verify(passwordEncoder, never()).encode(any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 재설정 인증 실패 - 잘못된 인증코드")
    void verifyAndResetPassword_Fail_InvalidVerificationCode() {
        // given
        AdminPasswordResetVerificationDTO verificationRequest = new AdminPasswordResetVerificationDTO(
                targetAdmin.getEmail(),
                "wrongCode",
                "newPassword123",
                "newPassword123"
        );

        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.of(targetAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.verifyAndResetPassword(verificationRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잘못된 인증코드이거나 만료된 인증코드입니다.");

        verify(passwordEncoder, never()).encode(any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 재설정 인증 실패 - 비밀번호 불일치")
    void verifyAndResetPassword_Fail_PasswordMismatch() {
        // given
        AdminPasswordResetVerificationDTO verificationRequest = new AdminPasswordResetVerificationDTO(
                targetAdmin.getEmail(),
                "123456",
                "newPassword123",
                "differentPassword123" // 확인 비밀번호 불일치
        );

        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.of(targetAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.verifyAndResetPassword(verificationRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");

        verify(passwordEncoder, never()).encode(any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("계정 초기화 로직 검증")
    void resetAdminAccount_Logic() {
        // given
        String verificationCode = "123456";
        boolean originalActiveStatus = targetAdmin.getIsActive();
        boolean originalFirstLoginStatus = targetAdmin.getIsFirstLogin();

        given(adminRepository.findByEmail(resetRequest.email())).willReturn(Optional.of(targetAdmin));
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        // when
        adminPasswordResetService.requestPasswordReset(resetRequest);

        // then
        // 계정 상태가 올바르게 초기화되었는지 확인
        assertThat(targetAdmin.getIsActive()).isFalse(); // 비활성화
        assertThat(targetAdmin.getIsFirstLogin()).isTrue(); // 첫 로그인 상태로 되돌림
        // 비밀번호는 인증 후에 새로 생성되므로 여기서는 변경되지 않음
        assertThat(targetAdmin.getPassword()).isEqualTo("encodedOldPassword");
    }

    @Test
    @DisplayName("비밀번호 재설정 완료 후 계정 상태 검증")
    void resetPassword_Logic() {
        // given
        String newPassword = "newPassword123";
        String confirmPassword = "newPassword123";

        AdminPasswordResetVerificationDTO verificationRequest = new AdminPasswordResetVerificationDTO(
                targetAdmin.getEmail(),
                "123456",
                newPassword,
                confirmPassword
        );

        // 비활성화된 상태로 설정
        targetAdmin.setIsActive(false);
        targetAdmin.setIsFirstLogin(true);

        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.of(targetAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .willReturn(true);
        given(passwordEncoder.encode(newPassword)).willReturn("encodedNewPassword");
        given(adminUtils.getLoginRedirectUrl()).willReturn("http://localhost:8080/v1/admin/login");

        // when
        adminPasswordResetService.verifyAndResetPassword(verificationRequest);

        // then
        // 비밀번호 재설정 후 계정 상태 확인
        assertThat(targetAdmin.getPassword()).isEqualTo("encodedNewPassword");
        assertThat(targetAdmin.getIsActive()).isTrue(); // 다시 활성화
        assertThat(targetAdmin.getIsFirstLogin()).isFalse(); // 비밀번호 재설정 완료로 간주
    }

    @Test
    @DisplayName("인증코드 생성 및 Redis 저장 검증")
    void verificationCodeHandling() {
        // given
        String verificationCode = "654321";

        given(adminRepository.findByEmail(resetRequest.email())).willReturn(Optional.of(targetAdmin));
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        // when
        adminPasswordResetService.requestPasswordReset(resetRequest);

        // then
        // 인증코드 생성 검증
        verify(adminUtils).generateVerificationCode();

        // Redis에 인증코드 저장 검증
        verify(redisVerificationCodeService).saveVerificationCode(targetAdmin.getEmail(), verificationCode);

        // 만료시간 계산 검증
        verify(redisVerificationCodeService).calculateExpiryTime();

        // 이메일 발송에 올바른 인증코드 전달 검증
        verify(adminUtils).sendPasswordResetEmail(
                eq(targetAdmin.getEmail()),
                eq(targetAdmin.getName()),
                eq(verificationCode),
                eq(resetRequest.requestedBy())
        );
    }

    @Test
    @DisplayName("이메일 발송 내용 검증")
    void emailSending_Verification() {
        // given
        String verificationCode = "999888";

        given(adminRepository.findByEmail(resetRequest.email())).willReturn(Optional.of(targetAdmin));
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        // when
        adminPasswordResetService.requestPasswordReset(resetRequest);

        // then
        // 정확한 파라미터로 이메일 발송 메서드가 호출되었는지 확인
        verify(adminUtils).sendPasswordResetEmail(
                targetAdmin.getEmail(),
                targetAdmin.getName(),
                verificationCode,
                resetRequest.requestedBy()
        );
    }

    @Test
    @DisplayName("비밀번호 검증 로직 테스트")
    void validatePasswordMatch_Logic() {
        // given
        AdminPasswordResetVerificationDTO mismatchRequest = new AdminPasswordResetVerificationDTO(
                targetAdmin.getEmail(),
                "123456",
                "password1",
                "password2" // 불일치
        );

        given(adminRepository.findByEmail(mismatchRequest.email())).willReturn(Optional.of(targetAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(mismatchRequest.email(), mismatchRequest.verificationCode()))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> adminPasswordResetService.verifyAndResetPassword(mismatchRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("비밀번호 암호화 검증")
    void passwordEncryption_Verification() {
        // given
        String newPassword = "myNewPassword123";
        String encodedPassword = "encodedMyNewPassword123";

        AdminPasswordResetVerificationDTO verificationRequest = new AdminPasswordResetVerificationDTO(
                targetAdmin.getEmail(),
                "123456",
                newPassword,
                newPassword
        );

        given(adminRepository.findByEmail(verificationRequest.email())).willReturn(Optional.of(targetAdmin));
        given(redisVerificationCodeService.verifyAndDeleteCode(verificationRequest.email(), verificationRequest.verificationCode()))
                .willReturn(true);
        given(passwordEncoder.encode(newPassword)).willReturn(encodedPassword);
        given(adminUtils.getLoginRedirectUrl()).willReturn("http://localhost:8080/v1/admin/login");

        // when
        adminPasswordResetService.verifyAndResetPassword(verificationRequest);

        // then
        // 올바른 비밀번호로 암호화 호출 확인
        verify(passwordEncoder).encode(newPassword);

        // 암호화된 비밀번호가 저장되었는지 확인
        assertThat(targetAdmin.getPassword()).isEqualTo(encodedPassword);
    }

    @Test
    @DisplayName("다양한 부서 관리자 비밀번호 초기화")
    void requestPasswordReset_DifferentDepartments() {
        // given
        Admins csAdmin = Admins.builder()
                .id("admin-2")
                .email("cs@example.com")
                .name("고객서비스 관리자")
                .password("encodedPassword")
                .department(Department.CUSTOMER_SERVICE)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(true)
                .isFirstLogin(false)
                .build();

        AdminPasswordResetRequestDTO csResetRequest = new AdminPasswordResetRequestDTO(
                "cs@example.com",
                "admin@example.com"
        );

        String verificationCode = "111222";

        given(adminRepository.findByEmail(csResetRequest.email())).willReturn(Optional.of(csAdmin));
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        // when
        AdminPasswordResetResponseDTO response = adminPasswordResetService.requestPasswordReset(csResetRequest);

        // then
        assertThat(response.email()).isEqualTo("cs@example.com");
        assertThat(response.name()).isEqualTo("고객서비스 관리자");

        // 부서는 변경되지 않음을 확인
        assertThat(csAdmin.getDepartment()).isEqualTo(Department.CUSTOMER_SERVICE);
        assertThat(csAdmin.getIsActive()).isFalse(); // 비활성화됨
        assertThat(csAdmin.getIsFirstLogin()).isTrue(); // 첫 로그인 상태로 됨

        // 이메일 발송 확인
        verify(adminUtils).sendPasswordResetEmail(
                csAdmin.getEmail(),
                csAdmin.getName(),
                verificationCode,
                csResetRequest.requestedBy()
        );
    }

    @Test
    @DisplayName("Redis TTL 만료 시간 검증")
    void redisExpiryTime_Verification() {
        // given
        String verificationCode = "123456";
        ZonedDateTime customExpiryTime = ZonedDateTime.now().plusHours(2);

        given(adminRepository.findByEmail(resetRequest.email())).willReturn(Optional.of(targetAdmin));
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(customExpiryTime);

        // when
        AdminPasswordResetResponseDTO response = adminPasswordResetService.requestPasswordReset(resetRequest);

        // then
        assertThat(response.verificationCodeExpiry()).isEqualTo(customExpiryTime);

        // Redis 서비스의 만료시간 계산 메서드가 호출되었는지 확인
        verify(redisVerificationCodeService).calculateExpiryTime();
    }
}