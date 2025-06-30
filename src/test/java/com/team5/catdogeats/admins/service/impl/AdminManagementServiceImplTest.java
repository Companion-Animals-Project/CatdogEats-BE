package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminInfoResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminListResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminStatsDTO;
import com.team5.catdogeats.admins.domain.enums.AdminRole;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.RedisVerificationCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 관리 서비스 테스트")
class AdminManagementServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private RedisVerificationCodeService redisVerificationCodeService;

    @InjectMocks
    private AdminManagementServiceImpl adminManagementService;

    private List<Admins> testAdmins;
    private Page<Admins> testAdminPage;

    @BeforeEach
    void setUp() {
        testAdmins = Arrays.asList(
                createAdmin("1", "active@test.com", "활성 관리자", true, false),
                createAdmin("2", "pending@test.com", "대기 관리자", false, true),
                createAdmin("3", "inactive@test.com", "비활성 관리자", false, false)
        );

        testAdminPage = new PageImpl<>(testAdmins, PageRequest.of(0, 10), testAdmins.size());
    }

    private Admins createAdmin(String id, String email, String name, boolean isActive, boolean isFirstLogin) {
        return Admins.builder()
                .id(id)
                .email(email)
                .name(name)
                .password("encodedPassword")
                .department(Department.DEVELOPMENT)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(isActive)
                .isFirstLogin(isFirstLogin)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("관리자 목록 조회 - 전체 조회")
    void getAdminList_AllAdmins() {
        // given
        when(adminRepository.findAll(any(Pageable.class))).thenReturn(testAdminPage);
        when(adminRepository.count()).thenReturn(3L);
        when(adminRepository.countActiveAdmins()).thenReturn(1L);
        when(adminRepository.countDeletedAdmins()).thenReturn(0L);
        when(adminRepository.findAllInactiveAdmins()).thenReturn(Arrays.asList(testAdmins.get(1), testAdmins.get(2)));

        // Redis 인증코드 상태 설정
        when(redisVerificationCodeService.hasVerificationCode("pending@test.com")).thenReturn(true);
        when(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).thenReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "all", null, null);

        // then
        assertThat(result.admins()).hasSize(3);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.currentPage()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(10);

        AdminStatsDTO stats = result.stats();
        assertThat(stats.totalCount()).isEqualTo(3);
        assertThat(stats.activeCount()).isEqualTo(1);
        assertThat(stats.pendingCount()).isEqualTo(1);
        assertThat(stats.inactiveCount()).isEqualTo(1);
        assertThat(stats.deletedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("관리자 목록 조회 - 활성 관리자만")
    void getAdminList_ActiveOnly() {
        // given
        List<Admins> activeAdmins = Arrays.asList(testAdmins.get(0));
        Page<Admins> activeAdminPage = new PageImpl<>(activeAdmins, PageRequest.of(0, 10), 1);

        when(adminRepository.findActiveAdmins(any(Pageable.class))).thenReturn(activeAdminPage);
        when(adminRepository.count()).thenReturn(3L);
        when(adminRepository.countActiveAdmins()).thenReturn(1L);
        when(adminRepository.countDeletedAdmins()).thenReturn(0L);
        when(adminRepository.findAllInactiveAdmins()).thenReturn(Arrays.asList(testAdmins.get(1), testAdmins.get(2)));

        when(redisVerificationCodeService.hasVerificationCode("pending@test.com")).thenReturn(true);
        when(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).thenReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "active", null, null);

        // then
        assertThat(result.admins()).hasSize(1);
        assertThat(result.admins().get(0).email()).isEqualTo("active@test.com");
        assertThat(result.admins().get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("관리자 목록 조회 - 검색어로 필터링")
    void getAdminList_WithSearch() {
        // given
        String searchTerm = "활성";
        List<Admins> searchResults = Arrays.asList(testAdmins.get(0));
        Page<Admins> searchPage = new PageImpl<>(searchResults, PageRequest.of(0, 10), 1);

        when(adminRepository.findByNameOrEmailContainingIgnoreCase(eq(searchTerm), any(Pageable.class)))
                .thenReturn(searchPage);
        when(adminRepository.count()).thenReturn(3L);
        when(adminRepository.countActiveAdmins()).thenReturn(1L);
        when(adminRepository.countDeletedAdmins()).thenReturn(0L);
        when(adminRepository.findAllInactiveAdmins()).thenReturn(Arrays.asList(testAdmins.get(1), testAdmins.get(2)));

        when(redisVerificationCodeService.hasVerificationCode("pending@test.com")).thenReturn(true);
        when(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).thenReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "all", searchTerm, null);

        // then
        assertThat(result.admins()).hasSize(1);
        assertThat(result.admins().get(0).name()).contains("활성");
        verify(adminRepository).findByNameOrEmailContainingIgnoreCase(eq(searchTerm), any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 목록 조회 - 검색어 + 상태 필터링")
    void getAdminList_WithSearchAndStatus() {
        // given
        String searchTerm = "관리자";
        List<Admins> activeResults = Arrays.asList(testAdmins.get(0));
        Page<Admins> searchPage = new PageImpl<>(activeResults, PageRequest.of(0, 10), 1);

        when(adminRepository.findActiveAdminsBySearch(eq(searchTerm), any(Pageable.class)))
                .thenReturn(searchPage);
        when(adminRepository.count()).thenReturn(3L);
        when(adminRepository.countActiveAdmins()).thenReturn(1L);
        when(adminRepository.countDeletedAdmins()).thenReturn(0L);
        when(adminRepository.findAllInactiveAdmins()).thenReturn(Arrays.asList(testAdmins.get(1), testAdmins.get(2)));

        when(redisVerificationCodeService.hasVerificationCode("pending@test.com")).thenReturn(true);
        when(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).thenReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "active", searchTerm, null);

        // then
        assertThat(result.admins()).hasSize(1);
        assertThat(result.admins().get(0).isActive()).isTrue();
        verify(adminRepository).findActiveAdminsBySearch(eq(searchTerm), any(Pageable.class));
    }

    @Test
    @DisplayName("관리자 통계 계산")
    void calculateAdminStats() {
        // given
        when(adminRepository.count()).thenReturn(10L);
        when(adminRepository.countActiveAdmins()).thenReturn(7L);
        when(adminRepository.countDeletedAdmins()).thenReturn(2L);

        List<Admins> inactiveAdmins = Arrays.asList(
                createAdmin("pending1", "pending1@test.com", "대기1", false, true),
                createAdmin("pending2", "pending2@test.com", "대기2", false, true),
                createAdmin("inactive1", "inactive1@test.com", "비활성1", false, false)
        );
        when(adminRepository.findAllInactiveAdmins()).thenReturn(inactiveAdmins);

        // Redis 인증코드 상태 설정 (2개는 pending, 1개는 inactive)
        when(redisVerificationCodeService.hasVerificationCode("pending1@test.com")).thenReturn(true);
        when(redisVerificationCodeService.hasVerificationCode("pending2@test.com")).thenReturn(true);
        when(redisVerificationCodeService.hasVerificationCode("inactive1@test.com")).thenReturn(false);

        // when
        AdminStatsDTO result = adminManagementService.calculateAdminStats();

        // then
        assertThat(result.totalCount()).isEqualTo(10L);
        assertThat(result.activeCount()).isEqualTo(7L);
        assertThat(result.pendingCount()).isEqualTo(2L); // Redis에 인증코드가 있는 비활성 계정
        assertThat(result.inactiveCount()).isEqualTo(1L); // Redis에 인증코드가 없는 비활성 계정
        assertThat(result.deletedCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Redis 인증코드 정보가 포함된 DTO 변환 - 대기 상태")
    void convertToDTO_WithPendingVerificationCode() {
        // given
        Admins pendingAdmin = createAdmin("pending", "pending@test.com", "대기 관리자", false, true);
        when(redisVerificationCodeService.hasVerificationCode("pending@test.com")).thenReturn(true);
        when(redisVerificationCodeService.getVerificationCodeTTL("pending@test.com")).thenReturn(3600L); // 1시간

        when(adminRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(pendingAdmin)));
        when(adminRepository.count()).thenReturn(1L);
        when(adminRepository.countActiveAdmins()).thenReturn(0L);
        when(adminRepository.countDeletedAdmins()).thenReturn(0L);
        when(adminRepository.findAllInactiveAdmins()).thenReturn(Arrays.asList(pendingAdmin));

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "all", null, null);

        // then
        assertThat(result.admins()).hasSize(1);
        AdminInfoResponseDTO adminInfo = result.admins().get(0);
        assertThat(adminInfo.verificationCode()).isEqualTo("EXISTS");
        assertThat(adminInfo.verificationCodeExpiry()).isNotNull();
        assertThat(adminInfo.verificationCodeExpiry()).isAfter(ZonedDateTime.now());
    }

    @Test
    @DisplayName("Redis 인증코드 정보가 포함된 DTO 변환 - 비활성 상태")
    void convertToDTO_WithoutVerificationCode() {
        // given
        Admins inactiveAdmin = createAdmin("inactive", "inactive@test.com", "비활성 관리자", false, false);
        when(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).thenReturn(false);

        when(adminRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(inactiveAdmin)));
        when(adminRepository.count()).thenReturn(1L);
        when(adminRepository.countActiveAdmins()).thenReturn(0L);
        when(adminRepository.countDeletedAdmins()).thenReturn(0L);
        when(adminRepository.findAllInactiveAdmins()).thenReturn(Arrays.asList(inactiveAdmin));

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "all", null, null);

        // then
        assertThat(result.admins()).hasSize(1);
        AdminInfoResponseDTO adminInfo = result.admins().get(0);
        assertThat(adminInfo.verificationCode()).isNull();
        assertThat(adminInfo.verificationCodeExpiry()).isNull();
    }

    @Test
    @DisplayName("Redis 인증코드 정보가 포함된 DTO 변환 - 활성 상태")
    void convertToDTO_ActiveAdmin() {
        // given
        Admins activeAdmin = createAdmin("active", "active@test.com", "활성 관리자", true, false);

        when(adminRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Arrays.asList(activeAdmin)));
        when(adminRepository.count()).thenReturn(1L);
        when(adminRepository.countActiveAdmins()).thenReturn(1L);
        when(adminRepository.countDeletedAdmins()).thenReturn(0L);
        when(adminRepository.findAllInactiveAdmins()).thenReturn(Arrays.asList());

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "all", null, null);

        // then
        assertThat(result.admins()).hasSize(1);
        AdminInfoResponseDTO adminInfo = result.admins().get(0);
        assertThat(adminInfo.isActive()).isTrue();
        assertThat(adminInfo.verificationCode()).isNull(); // 활성 계정은 인증코드 확인 안함
        assertThat(adminInfo.verificationCodeExpiry()).isNull();
    }

    @Test
    @DisplayName("페이지네이션 처리")
    void getAdminList_Pagination() {
        // given
        int page = 1;
        int size = 5;
        List<Admins> pageAdmins = Arrays.asList(
                createAdmin("4", "admin4@test.com", "관리자4", true, false),
                createAdmin("5", "admin5@test.com", "관리자5", true, false)
        );
        Page<Admins> pagedResult = new PageImpl<>(pageAdmins, PageRequest.of(page, size), 12); // 총 12개

        when(adminRepository.findAll(any(Pageable.class))).thenReturn(pagedResult);
        when(adminRepository.count()).thenReturn(12L);
        when(adminRepository.countActiveAdmins()).thenReturn(10L);
        when(adminRepository.countDeletedAdmins()).thenReturn(1L);
        when(adminRepository.findAllInactiveAdmins()).thenReturn(Arrays.asList());

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(page, size, "all", null, null);

        // then
        assertThat(result.admins()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(12);
        assertThat(result.totalPages()).isEqualTo(3); // 12 / 5 = 2.4 → 3페이지
        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(5);
    }

    @Test
    @DisplayName("빈 검색 결과 처리")
    void getAdminList_EmptyResult() {
        // given
        Page<Admins> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);

        when(adminRepository.findByNameOrEmailContainingIgnoreCase(eq("존재하지않는검색어"), any(Pageable.class)))
                .thenReturn(emptyPage);
        when(adminRepository.count()).thenReturn(0L);
        when(adminRepository.countActiveAdmins()).thenReturn(0L);
        when(adminRepository.countDeletedAdmins()).thenReturn(0L);
        when(adminRepository.findAllInactiveAdmins()).thenReturn(Arrays.asList());

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "all", "존재하지않는검색어", null);

        // then
        assertThat(result.admins()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
        assertThat(result.totalPages()).isEqualTo(0);
        assertThat(result.stats().totalCount()).isEqualTo(0);
    }
}