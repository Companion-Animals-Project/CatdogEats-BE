package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminSoftDeleteRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminSoftDeleteResponseDTO;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSoftDeleteService 테스트")
class AdminSoftDeleteServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminSoftDeleteServiceImpl adminSoftDeleteService;

    // 테스트 상수
    private static final String SUPER_ADMIN_EMAIL = "super@admin.com";
    private static final String ADMIN_EMAIL = "admin@test.com";
    private static final String TARGET_EMAIL = "target@test.com";
    private static final String ADMIN_NAME = "관리자";

    private Admins mockAdmin;
    private AdminSoftDeleteRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        // 슈퍼관리자 이메일 설정
        ReflectionTestUtils.setField(adminSoftDeleteService, "superAdminEmail", SUPER_ADMIN_EMAIL);

        // Mock 관리자 객체 생성
        mockAdmin = Admins.builder()
                .email(TARGET_EMAIL)
                .name(ADMIN_NAME)
                .isDeleted(false)
                .build();

        // 유효한 요청 DTO 생성
        validRequest = new AdminSoftDeleteRequestDTO(
                TARGET_EMAIL,
                ADMIN_EMAIL,
                "퇴사"
        );
    }

    @Test
    @DisplayName("관리자 퇴사 처리 - 성공")
    void softDeleteAdmin_Success() {
        // Given
        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.of(mockAdmin));
        given(adminRepository.save(any(Admins.class))).willReturn(mockAdmin);

        // When
        AdminSoftDeleteResponseDTO result = adminSoftDeleteService.softDeleteAdmin(validRequest);

        // Then
        assertThat(result.email()).isEqualTo(TARGET_EMAIL);
        assertThat(result.name()).isEqualTo(ADMIN_NAME);
        assertThat(result.message()).isEqualTo("관리자가 성공적으로 퇴사 처리되었습니다.");

        verify(adminRepository).findByEmail(TARGET_EMAIL);
        verify(adminRepository).save(mockAdmin);
    }

    @Test
    @DisplayName("관리자 퇴사 처리 - 슈퍼관리자 보호")
    void softDeleteAdmin_SuperAdminProtection() {
        // Given
        AdminSoftDeleteRequestDTO superAdminRequest = new AdminSoftDeleteRequestDTO(
                SUPER_ADMIN_EMAIL,
                ADMIN_EMAIL,
                "퇴사"
        );

        // When & Then
        assertThatThrownBy(() -> adminSoftDeleteService.softDeleteAdmin(superAdminRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("슈퍼관리자 계정은 퇴사 처리할 수 없습니다.");

        verify(adminRepository, never()).findByEmail(any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 퇴사 처리 - 자기 자신 퇴사 방지")
    void softDeleteAdmin_SelfDeletionPrevention() {
        // Given
        AdminSoftDeleteRequestDTO selfRequest = new AdminSoftDeleteRequestDTO(
                TARGET_EMAIL,
                TARGET_EMAIL,  // 같은 이메일
                "퇴사"
        );

        // When & Then
        assertThatThrownBy(() -> adminSoftDeleteService.softDeleteAdmin(selfRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자신의 계정은 퇴사 처리할 수 없습니다.");

        verify(adminRepository, never()).findByEmail(any());
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 퇴사 처리 - 존재하지 않는 관리자")
    void softDeleteAdmin_AdminNotFound() {
        // Given
        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminSoftDeleteService.softDeleteAdmin(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 관리자입니다.");

        verify(adminRepository).findByEmail(TARGET_EMAIL);
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 퇴사 처리 - 이미 퇴사 처리된 관리자")
    void softDeleteAdmin_AlreadyDeleted() {
        // Given
        Admins deletedAdmin = Admins.builder()
                .email(TARGET_EMAIL)
                .name(ADMIN_NAME)
                .isDeleted(true)  // 이미 삭제됨
                .build();

        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.of(deletedAdmin));

        // When & Then
        assertThatThrownBy(() -> adminSoftDeleteService.softDeleteAdmin(validRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 퇴사 처리된 관리자입니다.");

        verify(adminRepository).findByEmail(TARGET_EMAIL);
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 퇴사 취소 - 성공")
    void undoSoftDelete_Success() {
        // Given
        Admins deletedAdmin = Admins.builder()
                .email(TARGET_EMAIL)
                .name(ADMIN_NAME)
                .isDeleted(true)
                .deletedAt(ZonedDateTime.now())
                .build();

        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.of(deletedAdmin));
        given(adminRepository.save(any(Admins.class))).willReturn(deletedAdmin);

        // When
        AdminSoftDeleteResponseDTO result = adminSoftDeleteService.undoSoftDelete(TARGET_EMAIL);

        // Then
        assertThat(result.email()).isEqualTo(TARGET_EMAIL);
        assertThat(result.name()).isEqualTo(ADMIN_NAME);
        assertThat(result.deletedAt()).isNull();
        assertThat(result.message()).isEqualTo("관리자의 퇴사 처리가 취소되었습니다.");

        verify(adminRepository).findByEmail(TARGET_EMAIL);
        verify(adminRepository).save(deletedAdmin);
    }

    @Test
    @DisplayName("관리자 퇴사 취소 - 존재하지 않는 관리자")
    void undoSoftDelete_AdminNotFound() {
        // Given
        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> adminSoftDeleteService.undoSoftDelete(TARGET_EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 관리자입니다.");

        verify(adminRepository).findByEmail(TARGET_EMAIL);
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 퇴사 취소 - 퇴사 처리되지 않은 관리자")
    void undoSoftDelete_NotDeleted() {
        // Given
        Admins activeAdmin = Admins.builder()
                .email(TARGET_EMAIL)
                .name(ADMIN_NAME)
                .isDeleted(false)  // 활성 상태
                .build();

        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.of(activeAdmin));

        // When & Then
        assertThatThrownBy(() -> adminSoftDeleteService.undoSoftDelete(TARGET_EMAIL))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("퇴사 처리되지 않은 관리자입니다.");

        verify(adminRepository).findByEmail(TARGET_EMAIL);
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 퇴사 처리 - 사유 없이 처리")
    void softDeleteAdmin_WithoutReason() {
        // Given
        AdminSoftDeleteRequestDTO requestWithoutReason = new AdminSoftDeleteRequestDTO(
                TARGET_EMAIL,
                ADMIN_EMAIL,
                null  // 사유 없음
        );

        given(adminRepository.findByEmail(TARGET_EMAIL)).willReturn(Optional.of(mockAdmin));
        given(adminRepository.save(any(Admins.class))).willReturn(mockAdmin);

        // When
        AdminSoftDeleteResponseDTO result = adminSoftDeleteService.softDeleteAdmin(requestWithoutReason);

        // Then
        assertThat(result.email()).isEqualTo(TARGET_EMAIL);
        assertThat(result.name()).isEqualTo(ADMIN_NAME);
        assertThat(result.message()).isEqualTo("관리자가 성공적으로 퇴사 처리되었습니다.");

        verify(adminRepository).findByEmail(TARGET_EMAIL);
        verify(adminRepository).save(mockAdmin);
    }
}