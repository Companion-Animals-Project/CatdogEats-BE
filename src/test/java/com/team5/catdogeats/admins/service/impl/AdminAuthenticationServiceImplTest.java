package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.domain.enums.AdminRole;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.repository.AdminSessionRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 인증 서비스 테스트")
class AdminAuthenticationServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AdminSessionRepository adminSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpSession session;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminAuthenticationServiceImpl adminAuthenticationService;

    private Admins testAdmin;
    private AdminLoginRequestDTO loginRequest;

    @BeforeEach
    void setUp() {
        // adminSessionKey 설정
        ReflectionTestUtils.setField(adminAuthenticationService, "adminSessionKey", "ADMIN_USER");

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

        loginRequest = new AdminLoginRequestDTO("test@admin.com", "password");
    }

    @Test
    @DisplayName("로그인 성공 - 일반 관리자")
    void login_Success() {
        // given
        String sessionId = "session-123";
        ZonedDateTime now = ZonedDateTime.now();

        given(adminRepository.findByEmail(loginRequest.email())).willReturn(Optional.of(testAdmin));
        given(passwordEncoder.matches(loginRequest.password(), testAdmin.getPassword())).willReturn(true);
        given(session.getId()).willReturn(sessionId);

        AdminSession mockSession = AdminSession.builder()
                .sessionId(sessionId)
                .adminId(testAdmin.getId())
                .build();
        given(adminSessionRepository.save(any(AdminSession.class))).willReturn(mockSession);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
                     mockStatic(SecurityContextHolder.class)) {

            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // when
            AdminLoginResponseDTO response = adminAuthenticationService.login(loginRequest, session);

            // then
            assertThat(response.email()).isEqualTo(testAdmin.getEmail());
            assertThat(response.name()).isEqualTo(testAdmin.getName());
            assertThat(response.department()).isEqualTo(testAdmin.getDepartment());
            assertThat(response.isFirstLogin()).isFalse();
            assertThat(response.redirectUrl()).isEqualTo("/v1/admin/dashboard");
            assertThat(response.message()).isEqualTo("로그인 성공");

            // Spring Security Authentication 설정 검증
            verify(securityContext).setAuthentication(any(UsernamePasswordAuthenticationToken.class));

            // AdminSession 저장 검증
            verify(adminSessionRepository).save(any(AdminSession.class));

            // 마지막 로그인 시간 업데이트 검증
            verify(adminRepository).save(testAdmin);
        }
    }

    @Test
    @DisplayName("로그인 성공 - 첫 로그인 사용자")
    void login_Success_FirstLogin() {
        // given
        testAdmin.setIsFirstLogin(true);
        String sessionId = "session-123";

        given(adminRepository.findByEmail(loginRequest.email())).willReturn(Optional.of(testAdmin));
        given(passwordEncoder.matches(loginRequest.password(), testAdmin.getPassword())).willReturn(true);
        given(session.getId()).willReturn(sessionId);

        AdminSession mockSession = AdminSession.builder()
                .sessionId(sessionId)
                .adminId(testAdmin.getId())
                .build();
        given(adminSessionRepository.save(any(AdminSession.class))).willReturn(mockSession);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
                     mockStatic(SecurityContextHolder.class)) {

            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

            // when
            AdminLoginResponseDTO response = adminAuthenticationService.login(loginRequest, session);

            // then
            assertThat(response.isFirstLogin()).isTrue();
            assertThat(response.redirectUrl()).isEqualTo("/v1/admin/change-password");
            assertThat(response.message()).isEqualTo("첫 로그인입니다. 보안을 위해 비밀번호를 변경해주세요.");
        }
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 이메일")
    void login_Fail_EmailNotFound() {
        // given
        given(adminRepository.findByEmail(loginRequest.email())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.login(loginRequest, session))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(adminSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인 실패 - 비활성화된 계정")
    void login_Fail_InactiveAccount() {
        // given
        testAdmin.setIsActive(false);
        given(adminRepository.findByEmail(loginRequest.email())).willReturn(Optional.of(testAdmin));

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.login(loginRequest, session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("계정이 활성화되지 않았습니다. 이메일을 확인해주세요.");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(adminSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그인 실패 - 잘못된 비밀번호")
    void login_Fail_WrongPassword() {
        // given
        given(adminRepository.findByEmail(loginRequest.email())).willReturn(Optional.of(testAdmin));
        given(passwordEncoder.matches(loginRequest.password(), testAdmin.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.login(loginRequest, session))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("이메일 또는 비밀번호가 올바르지 않습니다.");

        verify(adminSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // given
        String sessionId = "session-123";
        given(session.getId()).willReturn(sessionId);

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
                     mockStatic(SecurityContextHolder.class)) {

            // when
            adminAuthenticationService.logout(session);

            // then
            verify(adminSessionRepository).deleteById(sessionId);
            verify(session).invalidate();
            mockedSecurityContextHolder.verify(SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("비밀번호 변경 성공")
    void changePassword_Success() {
        // given
        String sessionId = "session-123";
        AdminPasswordChangeRequestDTO request = new AdminPasswordChangeRequestDTO(
                "oldPassword", "newPassword", "newPassword");

        AdminSession adminSession = AdminSession.builder()
                .sessionId(sessionId)
                .adminId(testAdmin.getId())
                .build();

        given(session.getId()).willReturn(sessionId);
        given(adminSessionRepository.findById(sessionId)).willReturn(Optional.of(adminSession));
        given(adminRepository.findById(testAdmin.getId())).willReturn(Optional.of(testAdmin));
        given(passwordEncoder.matches("oldPassword", testAdmin.getPassword())).willReturn(true);
        given(passwordEncoder.encode("newPassword")).willReturn("encodedNewPassword");

        // when
        adminAuthenticationService.changePassword(request, session);

        // then
        verify(adminRepository).save(testAdmin);
        assertThat(testAdmin.getPassword()).isEqualTo("encodedNewPassword");
        assertThat(testAdmin.getIsFirstLogin()).isFalse();
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 로그인되지 않은 상태")
    void changePassword_Fail_NotLoggedIn() {
        // given
        String sessionId = "session-123";
        AdminPasswordChangeRequestDTO request = new AdminPasswordChangeRequestDTO(
                "oldPassword", "newPassword", "newPassword");

        given(session.getId()).willReturn(sessionId);
        given(adminSessionRepository.findById(sessionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.changePassword(request, session))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("로그인이 필요합니다.");

        verify(passwordEncoder, never()).matches(any(), any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 틀림")
    void changePassword_Fail_WrongCurrentPassword() {
        // given
        String sessionId = "session-123";
        AdminPasswordChangeRequestDTO request = new AdminPasswordChangeRequestDTO(
                "wrongPassword", "newPassword", "newPassword");

        AdminSession adminSession = AdminSession.builder()
                .sessionId(sessionId)
                .adminId(testAdmin.getId())
                .build();

        given(session.getId()).willReturn(sessionId);
        given(adminSessionRepository.findById(sessionId)).willReturn(Optional.of(adminSession));
        given(adminRepository.findById(testAdmin.getId())).willReturn(Optional.of(testAdmin));
        given(passwordEncoder.matches("wrongPassword", testAdmin.getPassword())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.changePassword(request, session))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("현재 비밀번호가 올바르지 않습니다.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호 불일치")
    void changePassword_Fail_PasswordMismatch() {
        // given
        String sessionId = "session-123";
        AdminPasswordChangeRequestDTO request = new AdminPasswordChangeRequestDTO(
                "oldPassword", "newPassword", "differentPassword");

        AdminSession adminSession = AdminSession.builder()
                .sessionId(sessionId)
                .adminId(testAdmin.getId())
                .build();

        given(session.getId()).willReturn(sessionId);
        given(adminSessionRepository.findById(sessionId)).willReturn(Optional.of(adminSession));
        given(adminRepository.findById(testAdmin.getId())).willReturn(Optional.of(testAdmin));
        given(passwordEncoder.matches("oldPassword", testAdmin.getPassword())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> adminAuthenticationService.changePassword(request, session))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");

        verify(passwordEncoder, never()).encode(any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("세션 정보 조회 성공")
    void getSessionInfo_Success() {
        // given
        String sessionId = "session-123";
        AdminSession adminSession = AdminSession.builder()
                .sessionId(sessionId)
                .adminId(testAdmin.getId())
                .build();

        given(session.getId()).willReturn(sessionId);
        given(adminSessionRepository.findById(sessionId)).willReturn(Optional.of(adminSession));
        given(adminRepository.findById(testAdmin.getId())).willReturn(Optional.of(testAdmin));

        // when
        AdminInfo result = adminAuthenticationService.getSessionInfo(session);

        // then
        assertThat(result).isNotNull();
        assertThat(result.adminId()).isEqualTo(testAdmin.getId());
        assertThat(result.email()).isEqualTo(testAdmin.getEmail());
        assertThat(result.name()).isEqualTo(testAdmin.getName());
        assertThat(result.department()).isEqualTo(testAdmin.getDepartment());
        assertThat(result.isFirstLogin()).isEqualTo(testAdmin.getIsFirstLogin());
    }

    @Test
    @DisplayName("세션 정보 조회 실패 - 세션 없음")
    void getSessionInfo_Fail_NoSession() {
        // given
        String sessionId = "session-123";
        given(session.getId()).willReturn(sessionId);
        given(adminSessionRepository.findById(sessionId)).willReturn(Optional.empty());

        // when
        AdminInfo result = adminAuthenticationService.getSessionInfo(session);

        // then
        assertThat(result).isNull();
        verify(adminRepository, never()).findById(any());
    }

    @Test
    @DisplayName("로그인 상태 확인 - 로그인됨")
    void isLoggedIn_True() {
        // given
        String sessionId = "session-123";
        AdminSession adminSession = AdminSession.builder()
                .sessionId(sessionId)
                .adminId(testAdmin.getId())
                .build();

        given(session.getId()).willReturn(sessionId);
        given(adminSessionRepository.findById(sessionId)).willReturn(Optional.of(adminSession));

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
                     mockStatic(SecurityContextHolder.class)) {

            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getPrincipal()).willReturn("test@admin.com");

            // when
            boolean result = adminAuthenticationService.isLoggedIn(session);

            // then
            assertThat(result).isTrue();
        }
    }

    @Test
    @DisplayName("로그인 상태 확인 - 로그인되지 않음")
    void isLoggedIn_False() {
        // given
        String sessionId = "session-123";
        given(session.getId()).willReturn(sessionId);
        given(adminSessionRepository.findById(sessionId)).willReturn(Optional.empty());

        try (MockedStatic<SecurityContextHolder> mockedSecurityContextHolder =
                     mockStatic(SecurityContextHolder.class)) {

            mockedSecurityContextHolder.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            given(securityContext.getAuthentication()).willReturn(authentication);
            given(authentication.isAuthenticated()).willReturn(true);
            given(authentication.getPrincipal()).willReturn("test@admin.com");

            // when
            boolean result = adminAuthenticationService.isLoggedIn(session);

            // then
            assertThat(result).isFalse();
        }
    }
}