package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminInvitationRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminInvitationResponseDTO;
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

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 초대 서비스 테스트")
class AdminInvitationServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminUtils adminUtils;

    @Mock
    private RedisVerificationCodeService redisVerificationCodeService;

    @InjectMocks
    private AdminInvitationServiceImpl adminInvitationService;

    private AdminInvitationRequestDTO invitationRequest;
    private ZonedDateTime mockExpiryTime;

    @BeforeEach
    void setUp() {
        invitationRequest = new AdminInvitationRequestDTO(
                "test@example.com",
                "테스트 사용자",
                Department.DEVELOPMENT
        );

        mockExpiryTime = ZonedDateTime.now().plusHours(1);
    }

    @Test
    @DisplayName("관리자 초대 성공")
    void inviteAdmin_Success() {
        // given
        String verificationCode = "123456";
        String tempPassword = "tempPass123";

        when(adminRepository.existsByEmail(invitationRequest.email())).thenReturn(false);
        when(adminUtils.generateVerificationCode()).thenReturn(verificationCode);
        when(adminUtils.generateInitialPassword()).thenReturn(tempPassword);
        when(passwordEncoder.encode(tempPassword)).thenReturn("encodedTempPassword");
        when(redisVerificationCodeService.calculateExpiryTime()).thenReturn(mockExpiryTime);

        ArgumentCaptor<Admins> adminCaptor = ArgumentCaptor.forClass(Admins.class);
        when(adminRepository.save(adminCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        AdminInvitationResponseDTO response = adminInvitationService.inviteAdmin(invitationRequest);

        // then
        assertThat(response.email()).isEqualTo(invitationRequest.email());
        assertThat(response.name()).isEqualTo(invitationRequest.name());
        assertThat(response.department()).isEqualTo(invitationRequest.department());
        assertThat(response.verificationCodeExpiry()).isEqualTo(mockExpiryTime);
        assertThat(response.message()).isEqualTo("인증 이메일이 발송되었습니다.");

        // Redis에 인증코드 저장 검증
        verify(redisVerificationCodeService).saveVerificationCode(invitationRequest.email(), verificationCode);

        // 이메일 발송 검증
        verify(adminUtils).sendInvitationEmail(
                invitationRequest.email(),
                invitationRequest.name(),
                verificationCode,
                invitationRequest.department()
        );

        // 저장된 관리자 정보 검증
        Admins savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getEmail()).isEqualTo(invitationRequest.email());
        assertThat(savedAdmin.getName()).isEqualTo(invitationRequest.name());
        assertThat(savedAdmin.getDepartment()).isEqualTo(invitationRequest.department());
        assertThat(savedAdmin.getAdminRole()).isEqualTo(AdminRole.ROLE_ADMIN);
        assertThat(savedAdmin.getPassword()).isEqualTo("encodedTempPassword");
        assertThat(savedAdmin.getIsActive()).isFalse(); // 초대 시점에는 비활성
        assertThat(savedAdmin.getIsFirstLogin()).isTrue(); // 첫 로그인 상태
    }

    @Test
    @DisplayName("관리자 초대 실패 - ADMIN 부서는 등록 불가")
    void inviteAdmin_Fail_AdminDepartmentNotAllowed() {
        // given
        AdminInvitationRequestDTO adminDeptRequest = new AdminInvitationRequestDTO(
                "admin@example.com",
                "관리자",
                Department.ADMIN
        );

        // when & then
        assertThatThrownBy(() -> adminInvitationService.inviteAdmin(adminDeptRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ADMIN 부서는 직접 등록할 수 없습니다. 시스템에서 자동으로 생성됩니다.");

        // 다른 메서드들이 호출되지 않았음을 확인
        verify(adminRepository, never()).existsByEmail(any());
        verify(adminUtils, never()).generateVerificationCode();
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminUtils, never()).sendInvitationEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("관리자 초대 실패 - 이메일 중복")
    void inviteAdmin_Fail_EmailAlreadyExists() {
        // given
        when(adminRepository.existsByEmail(invitationRequest.email())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> adminInvitationService.inviteAdmin(invitationRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록된 이메일입니다: " + invitationRequest.email());

        // 중복 확인 후 다른 로직은 실행되지 않음을 확인
        verify(adminUtils, never()).generateVerificationCode();
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminRepository, never()).save(any());
        verify(adminUtils, never()).sendInvitationEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("각 부서별 초대 성공 테스트")
    void inviteAdmin_Success_AllDepartments() {
        // given
        Department[] allowedDepartments = {
                Department.DEVELOPMENT,
                Department.CUSTOMER_SERVICE,
                Department.OPERATIONS
        };

        for (Department department : allowedDepartments) {
            // 각 테스트마다 mock 리셋
            reset(adminRepository, adminUtils, redisVerificationCodeService);

            AdminInvitationRequestDTO request = new AdminInvitationRequestDTO(
                    "test-" + department.name().toLowerCase() + "@example.com",
                    "테스트 " + department.name(),
                    department
            );

            when(adminRepository.existsByEmail(request.email())).thenReturn(false);
            when(adminUtils.generateVerificationCode()).thenReturn("123456");
            when(adminUtils.generateInitialPassword()).thenReturn("tempPass123");
            when(passwordEncoder.encode("tempPass123")).thenReturn("encodedTempPassword");
            when(redisVerificationCodeService.calculateExpiryTime()).thenReturn(mockExpiryTime);
            when(adminRepository.save(any(Admins.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            AdminInvitationResponseDTO response = adminInvitationService.inviteAdmin(request);

            // then
            assertThat(response.department()).isEqualTo(department);
            assertThat(response.email()).isEqualTo(request.email());
            assertThat(response.name()).isEqualTo(request.name());

            verify(adminRepository).save(any(Admins.class));
            verify(redisVerificationCodeService).saveVerificationCode(eq(request.email()), eq("123456"));
            verify(adminUtils).sendInvitationEmail(
                    eq(request.email()),
                    eq(request.name()),
                    eq("123456"),
                    eq(department)
            );
        }
    }

    @Test
    @DisplayName("관리자 초대 시 이메일 발송 실패 케이스")
    void inviteAdmin_EmailSendFailure() {
        // given
        when(adminRepository.existsByEmail(invitationRequest.email())).thenReturn(false);
        when(adminUtils.generateVerificationCode()).thenReturn("123456");
        when(adminUtils.generateInitialPassword()).thenReturn("tempPass123");
        when(passwordEncoder.encode("tempPass123")).thenReturn("encodedTempPassword");
        when(adminRepository.save(any(Admins.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 이메일 발송 시 예외 발생
        doThrow(new RuntimeException("이메일 발송에 실패했습니다."))
                .when(adminUtils).sendInvitationEmail(any(), any(), any(), any());

        // when & then
        assertThatThrownBy(() -> adminInvitationService.inviteAdmin(invitationRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이메일 발송에 실패했습니다.");

        // Redis에는 저장되었지만 이메일 발송이 실패한 상황
        verify(redisVerificationCodeService).saveVerificationCode(eq(invitationRequest.email()), eq("123456"));
        verify(adminRepository).save(any(Admins.class));
    }
}