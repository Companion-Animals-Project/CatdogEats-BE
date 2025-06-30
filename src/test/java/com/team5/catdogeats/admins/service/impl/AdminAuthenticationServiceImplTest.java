package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.domain.enums.AdminRole;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.repository.AdminRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 인증 서비스 테스트")
class AdminAuthenticationServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpSession session;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AdminAuthenticationServiceImpl adminAuthenticationService;

    private Admins testAdmin;
    private AdminLoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);

        testAdmin = Admins.builder()
                .id("admin-1")
                .email("test@admin.com")
                .name("테스트 관리자")
                .password("encodedPassword")
                .department(Department.DEVELOPMENT)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(true)
                .isFirstLogin(false)
                .build();

        loginRequest = new AdminLoginRequestDTO("test@admin.com", "rawPassword");
    }

    @Test
    @DisplayName("정상 로그인 - 일반 사용자")
    void login_Success_RegularUser() {
        // given
        when(adminRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches(loginRequest.password(), testAdmin.getPassword())).thenReturn(true);

        // when
        AdminLoginResponseDTO response = adminAuthenticationService.login(loginRequest, session);

        // then
        assertThat(response.email()).isEqualTo(testAdmin.getEmail());
        assertThat(response.name()).isEqualTo(testAdmin.getName());
        assertThat(response.department()).isEqualTo(testAdmin.getDepartment());
        assertThat(response.isFirstLogin()).isFalse();
        assertThat(response.redirectUrl()).isEqualTo("/v1/admin/dashboard");
        assertThat(response.message()).isEqualTo("로그인 성공");

        // 세션 설정 검증
        ArgumentCaptor<AdminSessionInfo> sessionCaptor = ArgumentCaptor.forClass(AdminSessionInfo.class);
        verify(session).setAttribute(eq("ADMIN_USER"), sessionCaptor.capture());
        verify(session).setMaxInactiveInterval(1800); // 30분

        AdminSessionInfo sessionInfo = sessionCaptor.getValue();
        assertThat(sessionInfo.getAdminId()).isEqualTo(testAdmin.getId());
        assertThat(sessionInfo.getEmail()).isEqualTo(testAdmin.getEmail());
        assertThat(sessionInfo.getName()).isEqualTo(testAdmin.getName());
        assertThat(sessionInfo.getDepartment()).isEqualTo(testAdmin.getDepartment());
        assertThat(sessionInfo.isFirstLogin()).isFalse();

        // Security Context 설정 검증
        verify(securityContext).setAuthentication(any(Authentication.class));
        verify(adminRepository).save(testAdmin);
    }

    @Test
    @DisplayName("첫 로그인 사용자 - 비밀번호 변경 페이지로 리다이렉트")
    void login_Success_FirstLogin() {
        // given
        testAdmin.setIsFirstLogin(true);
        when(adminRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches(loginRequest.password(), testAdmin.getPassword())).thenReturn(true);

        // when
        AdminLoginResponseDTO response = adminAuthenticationService.login(loginRequest, session);

        // then
        assertThat(response.isFirstLogin()).isTrue();
        assertThat(response.redirectUrl()).isEqualTo("/v1/admin/change-password");
        assertThat(response.message()).isEqualTo("첫 로그인입니다. 보안을 위해 비밀번호를 변경해주세요.");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_Fail_EmailNotFound() {
        // given
        when(adminRepository.findByEmail(loginRequest.email())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.login(loginRequest, session))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(session, never()).setAttribute(any(), any());
    }

    @Test
    @DisplayName("로그인 실패 - 비활성화된 계정")
    void login_Fail_InactiveAccount() {
        // given
        testAdmin.setIsActive(false);
        when(adminRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testAdmin));

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.login(loginRequest, session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("계정이 활성화되지 않았습니다. 이메일을 확인해주세요.");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(session, never()).setAttribute(any(), any());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_Fail_WrongPassword() {
        // given
        when(adminRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches(loginRequest.password(), testAdmin.getPassword())).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.login(loginRequest, session))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(session, never()).setAttribute(any(), any());
    }

    @Test
    @DisplayName("로그아웃 성공 - SecurityContextHolder.clearContext() 검증 포함")
    void logout_Success_WithStaticMock() {
        // given
        AdminSessionInfo sessionInfo = AdminSessionInfo.builder()
                .adminId("admin-1")
                .email("test@admin.com")
                .name("테스트 관리자")
                .build();
        when(session.getAttribute("ADMIN_USER")).thenReturn(sessionInfo);

        // when & then
        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
                     mockStatic(SecurityContextHolder.class)) {

            adminAuthenticationService.logout(session);

            verify(session).invalidate();
            mockedSecurityContextHolder.verify(SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_Success() {
        // given
        AdminSessionInfo sessionInfo = AdminSessionInfo.builder()
                .adminId("admin-1")
                .email("test@admin.com")
                .name("테스트 관리자")
                .isFirstLogin(true)
                .build();
        when(session.getAttribute("ADMIN_USER")).thenReturn(sessionInfo);
        when(adminRepository.findById("admin-1")).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("oldPassword", testAdmin.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        AdminPasswordChangeRequestDTO request = new AdminPasswordChangeRequestDTO(
                "oldPassword", "newPassword", "newPassword");

        // when
        adminAuthenticationService.changePassword(request, session);

        // then
        verify(adminRepository).save(testAdmin);
        assertThat(testAdmin.getPassword()).isEqualTo("encodedNewPassword");
        assertThat(testAdmin.getIsFirstLogin()).isFalse();

        ArgumentCaptor<AdminSessionInfo> sessionCaptor = ArgumentCaptor.forClass(AdminSessionInfo.class);
        verify(session).setAttribute(eq("ADMIN_USER"), sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().isFirstLogin()).isFalse();
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 로그인되지 않은 상태")
    void changePassword_Fail_NotLoggedIn() {
        // given
        when(session.getAttribute("ADMIN_USER")).thenReturn(null);
        AdminPasswordChangeRequestDTO request = new AdminPasswordChangeRequestDTO(
                "oldPassword", "newPassword", "newPassword");

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.changePassword(request, session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("로그인이 필요합니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void changePassword_Fail_WrongCurrentPassword() {
        // given
        AdminSessionInfo sessionInfo = AdminSessionInfo.builder()
                .adminId("admin-1")
                .email("test@admin.com")
                .name("테스트 관리자")
                .build();
        when(session.getAttribute("ADMIN_USER")).thenReturn(sessionInfo);
        when(adminRepository.findById("admin-1")).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("wrongPassword", testAdmin.getPassword())).thenReturn(false);

        AdminPasswordChangeRequestDTO request = new AdminPasswordChangeRequestDTO(
                "wrongPassword", "newPassword", "newPassword");

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.changePassword(request, session))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("현재 비밀번호가 올바르지 않습니다.");
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호 확인 불일치")
    void changePassword_Fail_PasswordMismatch() {
        // given
        AdminSessionInfo sessionInfo = AdminSessionInfo.builder()
                .adminId("admin-1")
                .email("test@admin.com")
                .name("테스트 관리자")
                .build();
        when(session.getAttribute("ADMIN_USER")).thenReturn(sessionInfo);
        when(adminRepository.findById("admin-1")).thenReturn(Optional.of(testAdmin));
        when(passwordEncoder.matches("oldPassword", testAdmin.getPassword())).thenReturn(true);

        AdminPasswordChangeRequestDTO request = new AdminPasswordChangeRequestDTO(
                "oldPassword", "newPassword", "differentPassword");

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.changePassword(request, session))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
    }

    @Test
    @DisplayName("로그인 상태 확인 - 로그인된 상태")
    void isLoggedIn_True() {
        // given
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("test@admin.com");
        when(securityContext.getAuthentication()).thenReturn(auth);

        AdminSessionInfo sessionInfo = AdminSessionInfo.builder()
                .adminId("admin-1")
                .email("test@admin.com")
                .name("테스트 관리자")
                .build();
        when(session.getAttribute("ADMIN_USER")).thenReturn(sessionInfo);

        // when
        boolean result = adminAuthenticationService.isLoggedIn(session);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("로그인 상태 확인 - 로그인되지 않은 상태")
    void isLoggedIn_False() {
        // given
        when(securityContext.getAuthentication()).thenReturn(null);

        // when
        boolean result = adminAuthenticationService.isLoggedIn(session);

        // then
        assertThat(result).isFalse();
    }
}