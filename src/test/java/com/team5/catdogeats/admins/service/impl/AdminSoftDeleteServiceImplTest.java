package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminSoftDeleteRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminSoftDeleteResponseDTO;
import com.team5.catdogeats.admins.domain.enums.AdminRole;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.repository.AdminRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 퇴사 처리 서비스 테스트")
class AdminSoftDeleteServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminSoftDeleteServiceImpl adminSoftDeleteService;

    // 테스트 상수
    private static final String SUPER_ADMIN_EMAIL = "super@admin.com";
    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String TARGET_EMAIL = "target@test.com";
    private static final String ADMIN_NAME = "대상 관리자";
    private static final String DELETE_REASON = "퇴사";

    private Admins targetAdmin;
    private AdminSoftDeleteRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        // 슈퍼관리자 이메일 설정
        ReflectionTestUtils.setField(adminSoftDeleteService, "superAdminEmail", SUPER_ADMIN_EMAIL);

        // Mock 관리자 객체 생성
        targetAdmin = Admins.builder()
                .id("admin-1")
                .email(TARGET_EMAIL)
                .name(ADMIN_NAME)
                .department(Department.DEVELOPMENT)
                .adminRole(AdminRole.ROLE_ADMIN)
                .password("encodedPassword")
                .isActive(true)
                .isDeleted(false)
                .isFirstLogin(false)
                .build();

        // 유효한 요청 DTO 생성
        validRequest = new AdminSoftDeleteRequestDTO(
                TARGET_EMAIL,
                ADMIN_EMAIL,
                DELETE_REASON
        );
    }

    @Test
    @DisplayName("관리자 퇴사 처리 성공")
    void softDeleteAdmin_Success() {
        // given
        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.of(targetAdmin));
        given(adminRepository.save(any(Admins.class))).willReturn(targetAdmin);

        // when
        AdminSoftDeleteResponseDTO result = adminSoftDeleteService.softDeleteAdmin(validRequest);

        // then
        assertThat(result.email()).isEqualTo(TARGET_EMAIL);
        assertThat(result.name()).isEqualTo(ADMIN_NAME);
        assertThat(result.deletedAt()).isNotNull();
        assertThat(result.message()).isEqualTo("관리자가 성공적으로 퇴사 처리되었습니다.");

        // 관리자 상태 변경 확인
        assertThat(targetAdmin.getIsDeleted()).isTrue();
        assertThat(targetAdmin.getDeletedAt()).isNotNull();
        assertThat(targetAdmin.getDeleteReason()).isEqualTo(DELETE_REASON);

        // 저장 메서드 호출 확인
        verify(adminRepository).save(targetAdmin);
    }

    @Test
    @DisplayName("관리자 퇴사 처리 실패 - 존재하지 않는 관리자")
    void softDeleteAdmin_Fail_AdminNotFound() {
        // given
        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminSoftDeleteService.softDeleteAdmin(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 관리자입니다.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 퇴사 처리 실패 - 슈퍼관리자 보호")
    void softDeleteAdmin_Fail_SuperAdminProtection() {
        // given
        AdminSoftDeleteRequestDTO superAdminRequest = new AdminSoftDeleteRequestDTO(
                SUPER_ADMIN_EMAIL,
                ADMIN_EMAIL,
                DELETE_REASON
        );

        // when & then
        assertThatThrownBy(() -> adminSoftDeleteService.softDeleteAdmin(superAdminRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("슈퍼관리자 계정은 퇴사 처리할 수 없습니다.");

        verify(adminRepository, never()).findByEmail(any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 퇴사 처리 실패 - 자기 자신 퇴사 방지")
    void softDeleteAdmin_Fail_SelfDeletion() {
        // given
        AdminSoftDeleteRequestDTO selfDeleteRequest = new AdminSoftDeleteRequestDTO(
                TARGET_EMAIL,
                TARGET_EMAIL, // 자기 자신
                DELETE_REASON
        );

        // when & then
        assertThatThrownBy(() -> adminSoftDeleteService.softDeleteAdmin(selfDeleteRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자신의 계정은 퇴사 처리할 수 없습니다.");

        verify(adminRepository, never()).findByEmail(any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 퇴사 처리 실패 - 이미 퇴사 처리된 관리자")
    void softDeleteAdmin_Fail_AlreadyDeleted() {
        // given
        targetAdmin.softDelete("이전 퇴사 사유"); // 이미 퇴사 처리됨
        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.of(targetAdmin));

        // when & then
        assertThatThrownBy(() -> adminSoftDeleteService.softDeleteAdmin(validRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 퇴사 처리된 관리자입니다.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 퇴사 처리 취소 성공")
    void undoSoftDelete_Success() {
        // given
        String adminEmail = TARGET_EMAIL;
        targetAdmin.softDelete(DELETE_REASON); // 퇴사 처리된 상태로 설정

        given(adminRepository.findByEmail(adminEmail)).willReturn(Optional.of(targetAdmin));
        given(adminRepository.save(any(Admins.class))).willReturn(targetAdmin);

        // when
        AdminSoftDeleteResponseDTO result = adminSoftDeleteService.undoSoftDelete(adminEmail);

        // then
        assertThat(result.email()).isEqualTo(TARGET_EMAIL);
        assertThat(result.name()).isEqualTo(ADMIN_NAME);
        assertThat(result.deletedAt()).isNull();
        assertThat(result.message()).isEqualTo("관리자의 퇴사 처리가 취소되었습니다.");

        // 관리자 상태 복원 확인
        assertThat(targetAdmin.getIsDeleted()).isFalse();
        assertThat(targetAdmin.getDeletedAt()).isNull();
        assertThat(targetAdmin.getDeleteReason()).isNull();

        // 저장 메서드 호출 확인
        verify(adminRepository).save(targetAdmin);
    }

    @Test
    @DisplayName("관리자 퇴사 처리 취소 실패 - 존재하지 않는 관리자")
    void undoSoftDelete_Fail_AdminNotFound() {
        // given
        String adminEmail = "nonexistent@test.com";
        given(adminRepository.findByEmail(adminEmail)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminSoftDeleteService.undoSoftDelete(adminEmail))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 관리자입니다.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 퇴사 처리 취소 실패 - 퇴사 처리되지 않은 관리자")
    void undoSoftDelete_Fail_NotDeleted() {
        // given
        String adminEmail = TARGET_EMAIL;
        // targetAdmin은 퇴사 처리되지 않은 상태 (isDeleted = false)

        given(adminRepository.findByEmail(adminEmail)).willReturn(Optional.of(targetAdmin));

        // when & then
        assertThatThrownBy(() -> adminSoftDeleteService.undoSoftDelete(adminEmail))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("퇴사 처리되지 않은 관리자입니다.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("퇴사 사유와 함께 처리 검증")
    void softDeleteAdmin_WithReason() {
        // given
        String customReason = "개인 사정으로 인한 퇴사";
        AdminSoftDeleteRequestDTO requestWithCustomReason = new AdminSoftDeleteRequestDTO(
                TARGET_EMAIL,
                ADMIN_EMAIL,
                customReason
        );

        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.of(targetAdmin));
        given(adminRepository.save(any(Admins.class))).willReturn(targetAdmin);

        // when
        AdminSoftDeleteResponseDTO result = adminSoftDeleteService.softDeleteAdmin(requestWithCustomReason);

        // then
        assertThat(result.email()).isEqualTo(TARGET_EMAIL);
        assertThat(result.name()).isEqualTo(ADMIN_NAME);
        assertThat(result.deletedAt()).isNotNull();

        // 퇴사 사유 확인
        assertThat(targetAdmin.getDeleteReason()).isEqualTo(customReason);
        assertThat(targetAdmin.getIsDeleted()).isTrue();
        assertThat(targetAdmin.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("퇴사 처리 시점 검증")
    void softDeleteAdmin_TimestampVerification() {
        // given
        ZonedDateTime beforeDeletion = ZonedDateTime.now();

        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.of(targetAdmin));
        given(adminRepository.save(any(Admins.class))).willReturn(targetAdmin);

        // when
        AdminSoftDeleteResponseDTO result = adminSoftDeleteService.softDeleteAdmin(validRequest);

        // then
        ZonedDateTime afterDeletion = ZonedDateTime.now();

        assertThat(targetAdmin.getDeletedAt()).isNotNull();
        assertThat(targetAdmin.getDeletedAt()).isAfter(beforeDeletion);
        assertThat(targetAdmin.getDeletedAt()).isBefore(afterDeletion);
        assertThat(result.deletedAt()).isEqualTo(targetAdmin.getDeletedAt());
    }

    @Test
    @DisplayName("퇴사 처리 취소 시점 검증")
    void undoSoftDelete_TimestampVerification() {
        // given
        targetAdmin.softDelete(DELETE_REASON); // 퇴사 처리
        ZonedDateTime deletedAt = targetAdmin.getDeletedAt();

        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.of(targetAdmin));
        given(adminRepository.save(any(Admins.class))).willReturn(targetAdmin);

        // when
        AdminSoftDeleteResponseDTO result = adminSoftDeleteService.undoSoftDelete(TARGET_EMAIL);

        // then
        assertThat(targetAdmin.getDeletedAt()).isNull();
        assertThat(targetAdmin.getDeleteReason()).isNull();
        assertThat(targetAdmin.getIsDeleted()).isFalse();
        assertThat(result.deletedAt()).isNull();
    }

    @Test
    @DisplayName("다양한 부서 관리자 퇴사 처리")
    void softDeleteAdmin_DifferentDepartments() {
        // given
        Admins devAdmin = createAdminWithDepartment("dev@test.com", Department.DEVELOPMENT);
        Admins csAdmin = createAdminWithDepartment("cs@test.com", Department.CUSTOMER_SERVICE);
        Admins opAdmin = createAdminWithDepartment("op@test.com", Department.OPERATIONS);

        // Development 부서 관리자 퇴사 처리
        given(adminRepository.findByEmail("dev@test.com")).willReturn(Optional.of(devAdmin));
        given(adminRepository.save(any(Admins.class))).willReturn(devAdmin);

        AdminSoftDeleteRequestDTO devRequest = new AdminSoftDeleteRequestDTO("dev@test.com", ADMIN_EMAIL, "퇴사");

        // when
        AdminSoftDeleteResponseDTO result = adminSoftDeleteService.softDeleteAdmin(devRequest);

        // then
        assertThat(result.email()).isEqualTo("dev@test.com");
        assertThat(devAdmin.getIsDeleted()).isTrue();
        assertThat(devAdmin.getDepartment()).isEqualTo(Department.DEVELOPMENT);
    }

    private Admins createAdminWithDepartment(String email, Department department) {
        return Admins.builder()
                .id("admin-" + department.name())
                .email(email)
                .name(department.name() + " 관리자")
                .department(department)
                .adminRole(AdminRole.ROLE_ADMIN)
                .password("encodedPassword")
                .isActive(true)
                .isDeleted(false)
                .isFirstLogin(false)
                .build();
    }
}