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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

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

        given(adminRepository.existsByEmail(invitationRequest.email())).willReturn(false);
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(adminUtils.generateInitialPassword()).willReturn(tempPassword);
        given(passwordEncoder.encode(tempPassword)).willReturn("encodedTempPassword");
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        ArgumentCaptor<Admins> adminCaptor = ArgumentCaptor.forClass(Admins.class);
        given(adminRepository.save(adminCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

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
        given(adminRepository.existsByEmail(invitationRequest.email())).willReturn(true);

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

    @ParameterizedTest
    @EnumSource(value = Department.class, names = {"DEVELOPMENT", "CUSTOMER_SERVICE", "OPERATIONS"})
    @DisplayName("각 부서별 초대 성공 테스트")
    void inviteAdmin_Success_AllDepartments(Department department) {
        // given
        AdminInvitationRequestDTO departmentRequest = new AdminInvitationRequestDTO(
                "test@" + department.name().toLowerCase() + ".com",
                "테스트 " + department.name(),
                department
        );

        String verificationCode = "123456";
        String tempPassword = "tempPass123";

        given(adminRepository.existsByEmail(departmentRequest.email())).willReturn(false);
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(adminUtils.generateInitialPassword()).willReturn(tempPassword);
        given(passwordEncoder.encode(tempPassword)).willReturn("encodedTempPassword");
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        ArgumentCaptor<Admins> adminCaptor = ArgumentCaptor.forClass(Admins.class);
        given(adminRepository.save(adminCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        AdminInvitationResponseDTO response = adminInvitationService.inviteAdmin(departmentRequest);

        // then
        assertThat(response.department()).isEqualTo(department);
        assertThat(response.email()).isEqualTo(departmentRequest.email());
        assertThat(response.name()).isEqualTo(departmentRequest.name());

        // 저장된 관리자 부서 확인
        Admins savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getDepartment()).isEqualTo(department);

        // 이메일 발송 검증
        verify(adminUtils).sendInvitationEmail(
                departmentRequest.email(),
                departmentRequest.name(),
                verificationCode,
                department
        );
    }

    @Test
    @DisplayName("인증코드 생성 및 Redis 저장 검증")
    void inviteAdmin_VerificationCodeHandling() {
        // given
        String verificationCode = "987654";
        String tempPassword = "tempPass123";

        given(adminRepository.existsByEmail(invitationRequest.email())).willReturn(false);
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(adminUtils.generateInitialPassword()).willReturn(tempPassword);
        given(passwordEncoder.encode(tempPassword)).willReturn("encodedTempPassword");
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);
        given(adminRepository.save(any(Admins.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        adminInvitationService.inviteAdmin(invitationRequest);

        // then
        // 인증코드 생성 검증
        verify(adminUtils).generateVerificationCode();

        // Redis에 인증코드 저장 검증
        verify(redisVerificationCodeService).saveVerificationCode(invitationRequest.email(), verificationCode);

        // 만료시간 계산 검증
        verify(redisVerificationCodeService).calculateExpiryTime();

        // 이메일 발송에 올바른 인증코드 전달 검증
        verify(adminUtils).sendInvitationEmail(
                eq(invitationRequest.email()),
                eq(invitationRequest.name()),
                eq(verificationCode),
                eq(invitationRequest.department())
        );
    }

    @Test
    @DisplayName("임시 비밀번호 생성 및 암호화 검증")
    void inviteAdmin_PasswordHandling() {
        // given
        String verificationCode = "123456";
        String tempPassword = "generatedTempPass";
        String encodedPassword = "encodedGeneratedTempPass";

        given(adminRepository.existsByEmail(invitationRequest.email())).willReturn(false);
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(adminUtils.generateInitialPassword()).willReturn(tempPassword);
        given(passwordEncoder.encode(tempPassword)).willReturn(encodedPassword);
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        ArgumentCaptor<Admins> adminCaptor = ArgumentCaptor.forClass(Admins.class);
        given(adminRepository.save(adminCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        adminInvitationService.inviteAdmin(invitationRequest);

        // then
        // 임시 비밀번호 생성 검증
        verify(adminUtils).generateInitialPassword();

        // 비밀번호 암호화 검증
        verify(passwordEncoder).encode(tempPassword);

        // 저장된 관리자의 비밀번호가 암호화된 것인지 확인
        Admins savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getPassword()).isEqualTo(encodedPassword);
    }

    @Test
    @DisplayName("생성된 관리자 계정 상태 검증")
    void inviteAdmin_AdminAccountStatus() {
        // given
        String verificationCode = "123456";
        String tempPassword = "tempPass123";

        given(adminRepository.existsByEmail(invitationRequest.email())).willReturn(false);
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(adminUtils.generateInitialPassword()).willReturn(tempPassword);
        given(passwordEncoder.encode(tempPassword)).willReturn("encodedTempPassword");
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        ArgumentCaptor<Admins> adminCaptor = ArgumentCaptor.forClass(Admins.class);
        given(adminRepository.save(adminCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        adminInvitationService.inviteAdmin(invitationRequest);

        // then
        Admins savedAdmin = adminCaptor.getValue();

        // 기본 정보 확인
        assertThat(savedAdmin.getEmail()).isEqualTo(invitationRequest.email());
        assertThat(savedAdmin.getName()).isEqualTo(invitationRequest.name());
        assertThat(savedAdmin.getDepartment()).isEqualTo(invitationRequest.department());

        // 권한 정보 확인
        assertThat(savedAdmin.getAdminRole()).isEqualTo(AdminRole.ROLE_ADMIN);

        // 계정 상태 확인
        assertThat(savedAdmin.getIsActive()).isFalse(); // 초대 시점에는 비활성
        assertThat(savedAdmin.getIsFirstLogin()).isTrue(); // 첫 로그인 상태
        assertThat(savedAdmin.getIsDeleted()).isFalse(); // 삭제되지 않음

        // 시간 정보 확인
        assertThat(savedAdmin.getCreatedAt()).isNotNull();
        assertThat(savedAdmin.getLastLoginAt()).isNull(); // 아직 로그인하지 않음
        assertThat(savedAdmin.getDeletedAt()).isNull();
        assertThat(savedAdmin.getDeleteReason()).isNull();
    }

    @Test
    @DisplayName("이메일 발송 내용 검증")
    void inviteAdmin_EmailContent() {
        // given
        String verificationCode = "555444";
        String tempPassword = "tempPass123";

        given(adminRepository.existsByEmail(invitationRequest.email())).willReturn(false);
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(adminUtils.generateInitialPassword()).willReturn(tempPassword);
        given(passwordEncoder.encode(tempPassword)).willReturn("encodedTempPassword");
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);
        given(adminRepository.save(any(Admins.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        adminInvitationService.inviteAdmin(invitationRequest);

        // then
        // 정확한 파라미터로 초대 이메일 발송 확인
        verify(adminUtils).sendInvitationEmail(
                invitationRequest.email(),
                invitationRequest.name(),
                verificationCode,
                invitationRequest.department()
        );
    }

    @Test
    @DisplayName("Redis TTL 만료 시간 응답 검증")
    void inviteAdmin_ExpiryTimeResponse() {
        // given
        String verificationCode = "123456";
        String tempPassword = "tempPass123";
        ZonedDateTime customExpiryTime = ZonedDateTime.now().plusHours(2);

        given(adminRepository.existsByEmail(invitationRequest.email())).willReturn(false);
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(adminUtils.generateInitialPassword()).willReturn(tempPassword);
        given(passwordEncoder.encode(tempPassword)).willReturn("encodedTempPassword");
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(customExpiryTime);
        given(adminRepository.save(any(Admins.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        AdminInvitationResponseDTO response = adminInvitationService.inviteAdmin(invitationRequest);

        // then
        assertThat(response.verificationCodeExpiry()).isEqualTo(customExpiryTime);

        // Redis 서비스의 만료시간 계산 메서드 호출 확인
        verify(redisVerificationCodeService).calculateExpiryTime();
    }

    @Test
    @DisplayName("관리자 초대 전체 플로우 검증")
    void inviteAdmin_CompleteFlow() {
        // given
        String verificationCode = "333777";
        String tempPassword = "flowTestPass";
        String encodedPassword = "encodedFlowTestPass";

        given(adminRepository.existsByEmail(invitationRequest.email())).willReturn(false);
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(adminUtils.generateInitialPassword()).willReturn(tempPassword);
        given(passwordEncoder.encode(tempPassword)).willReturn(encodedPassword);
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        ArgumentCaptor<Admins> adminCaptor = ArgumentCaptor.forClass(Admins.class);
        given(adminRepository.save(adminCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        AdminInvitationResponseDTO response = adminInvitationService.inviteAdmin(invitationRequest);

        // then
        // 1. 부서 검증이 먼저 실행됨 (ADMIN 부서가 아니므로 통과)

        // 2. 이메일 중복 검사 실행
        verify(adminRepository).existsByEmail(invitationRequest.email());

        // 3. 인증코드 생성
        verify(adminUtils).generateVerificationCode();

        // 4. Redis에 인증코드 저장
        verify(redisVerificationCodeService).saveVerificationCode(invitationRequest.email(), verificationCode);

        // 5. 관리자 계정 생성
        verify(adminUtils).generateInitialPassword();
        verify(passwordEncoder).encode(tempPassword);
        verify(adminRepository).save(any(Admins.class));

        // 6. 인증 이메일 발송
        verify(adminUtils).sendInvitationEmail(
                invitationRequest.email(),
                invitationRequest.name(),
                verificationCode,
                invitationRequest.department()
        );

        // 7. 응답 검증
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo(invitationRequest.email());
        assertThat(response.name()).isEqualTo(invitationRequest.name());
        assertThat(response.department()).isEqualTo(invitationRequest.department());
        assertThat(response.verificationCodeExpiry()).isEqualTo(mockExpiryTime);
        assertThat(response.message()).isEqualTo("인증 이메일이 발송되었습니다.");

        // 저장된 관리자 계정 검증
        Admins savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getEmail()).isEqualTo(invitationRequest.email());
        assertThat(savedAdmin.getName()).isEqualTo(invitationRequest.name());
        assertThat(savedAdmin.getDepartment()).isEqualTo(invitationRequest.department());
        assertThat(savedAdmin.getPassword()).isEqualTo(encodedPassword);
        assertThat(savedAdmin.getIsActive()).isFalse();
        assertThat(savedAdmin.getIsFirstLogin()).isTrue();
    }

    @Test
    @DisplayName("부서 검증 로직 상세 테스트")
    void validateDepartment_DetailedTest() {
        // ADMIN 부서 요청 생성
        AdminInvitationRequestDTO adminRequest1 = new AdminInvitationRequestDTO(
                "admin1@test.com", "관리자1", Department.ADMIN);
        AdminInvitationRequestDTO adminRequest2 = new AdminInvitationRequestDTO(
                "admin2@test.com", "관리자2", Department.ADMIN);

        // 여러 번 ADMIN 부서로 요청해도 모두 실패해야 함
        assertThatThrownBy(() -> adminInvitationService.inviteAdmin(adminRequest1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ADMIN 부서는 직접 등록할 수 없습니다. 시스템에서 자동으로 생성됩니다.");

        assertThatThrownBy(() -> adminInvitationService.inviteAdmin(adminRequest2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ADMIN 부서는 직접 등록할 수 없습니다. 시스템에서 자동으로 생성됩니다.");

        // 어떤 후속 로직도 실행되지 않았음을 확인
        verify(adminRepository, never()).existsByEmail(any());
        verify(adminUtils, never()).generateVerificationCode();
        verify(adminUtils, never()).generateInitialPassword();
        verify(passwordEncoder, never()).encode(any());
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminRepository, never()).save(any());
        verify(adminUtils, never()).sendInvitationEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("이메일 중복 검증 로직 상세 테스트")
    void validateEmailNotExists_DetailedTest() {
        // given
        String duplicateEmail = "duplicate@test.com";
        AdminInvitationRequestDTO duplicateRequest = new AdminInvitationRequestDTO(
                duplicateEmail, "중복 사용자", Department.DEVELOPMENT);

        given(adminRepository.existsByEmail(duplicateEmail)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> adminInvitationService.inviteAdmin(duplicateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 등록된 이메일입니다: " + duplicateEmail);

        // 이메일 중복 확인은 실행되지만, 그 이후 로직은 실행되지 않음
        verify(adminRepository).existsByEmail(duplicateEmail);
        verify(adminUtils, never()).generateVerificationCode();
        verify(adminUtils, never()).generateInitialPassword();
        verify(passwordEncoder, never()).encode(any());
        verify(redisVerificationCodeService, never()).saveVerificationCode(any(), any());
        verify(adminRepository, never()).save(any());
        verify(adminUtils, never()).sendInvitationEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("운영팀 관리자 초대 성공")
    void inviteAdmin_OperationsDepartment() {
        // given
        AdminInvitationRequestDTO operationsRequest = new AdminInvitationRequestDTO(
                "operations@test.com",
                "운영팀 관리자",
                Department.OPERATIONS
        );

        String verificationCode = "OP1234";
        String tempPassword = "operationsPass";

        given(adminRepository.existsByEmail(operationsRequest.email())).willReturn(false);
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(adminUtils.generateInitialPassword()).willReturn(tempPassword);
        given(passwordEncoder.encode(tempPassword)).willReturn("encodedOperationsPass");
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        ArgumentCaptor<Admins> adminCaptor = ArgumentCaptor.forClass(Admins.class);
        given(adminRepository.save(adminCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        AdminInvitationResponseDTO response = adminInvitationService.inviteAdmin(operationsRequest);

        // then
        assertThat(response.email()).isEqualTo("operations@test.com");
        assertThat(response.name()).isEqualTo("운영팀 관리자");
        assertThat(response.department()).isEqualTo(Department.OPERATIONS);

        Admins savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getDepartment()).isEqualTo(Department.OPERATIONS);
        assertThat(savedAdmin.getPassword()).isEqualTo("encodedOperationsPass");

        verify(adminUtils).sendInvitationEmail(
                "operations@test.com",
                "운영팀 관리자",
                verificationCode,
                Department.OPERATIONS
        );
    }

    @Test
    @DisplayName("고객서비스팀 관리자 초대 성공")
    void inviteAdmin_CustomerServiceDepartment() {
        // given
        AdminInvitationRequestDTO csRequest = new AdminInvitationRequestDTO(
                "cs@test.com",
                "고객서비스 관리자",
                Department.CUSTOMER_SERVICE
        );

        String verificationCode = "CS5678";
        String tempPassword = "csPass";

        given(adminRepository.existsByEmail(csRequest.email())).willReturn(false);
        given(adminUtils.generateVerificationCode()).willReturn(verificationCode);
        given(adminUtils.generateInitialPassword()).willReturn(tempPassword);
        given(passwordEncoder.encode(tempPassword)).willReturn("encodedCSPass");
        given(redisVerificationCodeService.calculateExpiryTime()).willReturn(mockExpiryTime);

        ArgumentCaptor<Admins> adminCaptor = ArgumentCaptor.forClass(Admins.class);
        given(adminRepository.save(adminCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        AdminInvitationResponseDTO response = adminInvitationService.inviteAdmin(csRequest);

        // then
        assertThat(response.email()).isEqualTo("cs@test.com");
        assertThat(response.name()).isEqualTo("고객서비스 관리자");
        assertThat(response.department()).isEqualTo(Department.CUSTOMER_SERVICE);

        Admins savedAdmin = adminCaptor.getValue();
        assertThat(savedAdmin.getDepartment()).isEqualTo(Department.CUSTOMER_SERVICE);
        assertThat(savedAdmin.getPassword()).isEqualTo("encodedCSPass");

        verify(adminUtils).sendInvitationEmail(
                "cs@test.com",
                "고객서비스 관리자",
                verificationCode,
                Department.CUSTOMER_SERVICE
        );
    }
}