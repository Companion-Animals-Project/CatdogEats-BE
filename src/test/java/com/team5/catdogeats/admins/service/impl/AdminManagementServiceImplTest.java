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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("관리자 관리 서비스 테스트")
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminManagementServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private RedisVerificationCodeService redisVerificationCodeService;

    @InjectMocks
    private AdminManagementServiceImpl adminManagementService;

    private List<Admins> testAdmins;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        // 테스트용 관리자 데이터 생성
        testAdmins = Arrays.asList(
                createTestAdmin("1", "active@test.com", "활성 관리자", true, false),
                createTestAdmin("2", "pending@test.com", "대기 관리자", false, false),
                createTestAdmin("3", "inactive@test.com", "비활성 관리자", false, false),
                createTestAdmin("4", "deleted@test.com", "퇴사 관리자", false, true)
        );

        // 모든 테스트 관리자에 대한 기본 Redis 설정
        lenient().when(redisVerificationCodeService.hasVerificationCode("active@test.com")).thenReturn(false);
        lenient().when(redisVerificationCodeService.hasVerificationCode("pending@test.com")).thenReturn(true);
        lenient().when(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).thenReturn(false);
        lenient().when(redisVerificationCodeService.hasVerificationCode("deleted@test.com")).thenReturn(false);
        lenient().when(redisVerificationCodeService.getVerificationCodeTTL("pending@test.com")).thenReturn(3600L);
    }

    private Admins createTestAdmin(String id, String email, String name, boolean isActive, boolean isDeleted) {
        Admins admin = Admins.builder()
                .id(id)
                .email(email)
                .name(name)
                .department(Department.DEVELOPMENT)
                .adminRole(AdminRole.ROLE_ADMIN)
                .password("encodedPassword")
                .isActive(isActive)
                .isFirstLogin(true)
                .isDeleted(isDeleted)
                .build();

        if (isDeleted) {
            admin.softDelete("퇴사");
        }

        return admin;
    }

    @Test
    @DisplayName("전체 관리자 목록 조회")
    void getAdminList_All() {
        // given
        List<Admins> activeAdmins = testAdmins.subList(0, 3); // 퇴사자 제외
        Page<Admins> adminPage = new PageImpl<>(activeAdmins, pageable, activeAdmins.size());

        given(adminRepository.findAll(pageable)).willReturn(adminPage);
        given(adminRepository.count()).willReturn(3L);
        given(adminRepository.countActiveAdmins()).willReturn(1L);
        given(adminRepository.countDeletedAdmins()).willReturn(1L);
        given(adminRepository.findAllInactiveAdmins()).willReturn(testAdmins.subList(1, 3));

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, null, null, null);

        // then
        assertThat(result.admins()).hasSize(3);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.currentPage()).isEqualTo(0);
        assertThat(result.pageSize()).isEqualTo(10);

        AdminStatsDTO stats = result.stats();
        assertThat(stats.totalCount()).isEqualTo(3);
        assertThat(stats.activeCount()).isEqualTo(1);
        assertThat(stats.pendingCount()).isEqualTo(1);
        assertThat(stats.inactiveCount()).isEqualTo(1);
        assertThat(stats.deletedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("활성 관리자만 조회")
    void getAdminList_ActiveOnly() {
        // given
        List<Admins> activeAdmins = List.of(testAdmins.get(0));
        Page<Admins> adminPage = new PageImpl<>(activeAdmins, pageable, activeAdmins.size());

        given(adminRepository.findActiveAdmins(pageable)).willReturn(adminPage);
        given(adminRepository.count()).willReturn(3L);
        given(adminRepository.countActiveAdmins()).willReturn(1L);
        given(adminRepository.countDeletedAdmins()).willReturn(1L);
        given(adminRepository.findAllInactiveAdmins()).willReturn(testAdmins.subList(1, 3));
        given(redisVerificationCodeService.hasVerificationCode("pending@test.com")).willReturn(true);
        given(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).willReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "active", null, null);

        // then
        assertThat(result.admins()).hasSize(1);
        assertThat(result.admins().get(0).email()).isEqualTo("active@test.com");
        assertThat(result.admins().get(0).isActive()).isTrue();
    }

    @Test
    @DisplayName("대기 상태 관리자만 조회 (Redis 인증코드 있음)")
    void getAdminList_PendingOnly() {
        // given
        List<Admins> inactiveAdmins = testAdmins.subList(1, 3);
        Page<Admins> adminPage = new PageImpl<>(inactiveAdmins, pageable, inactiveAdmins.size());

        given(adminRepository.findInactiveAdmins(pageable)).willReturn(adminPage);
        given(adminRepository.count()).willReturn(3L);
        given(adminRepository.countActiveAdmins()).willReturn(1L);
        given(adminRepository.countDeletedAdmins()).willReturn(1L);
        given(adminRepository.findAllInactiveAdmins()).willReturn(inactiveAdmins);

        // pending@test.com만 인증코드 있음
        given(redisVerificationCodeService.hasVerificationCode("pending@test.com")).willReturn(true);
        given(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).willReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "pending", null, null);

        // then
        assertThat(result.admins()).hasSize(1);
        assertThat(result.admins().get(0).email()).isEqualTo("pending@test.com");
        assertThat(result.admins().get(0).isActive()).isFalse();
    }

    @Test
    @DisplayName("비활성 상태 관리자만 조회 (Redis 인증코드 없음)")
    void getAdminList_InactiveOnly() {
        // given
        List<Admins> inactiveAdmins = testAdmins.subList(1, 3);
        Page<Admins> adminPage = new PageImpl<>(inactiveAdmins, pageable, inactiveAdmins.size());

        given(adminRepository.findInactiveAdmins(pageable)).willReturn(adminPage);
        given(adminRepository.count()).willReturn(3L);
        given(adminRepository.countActiveAdmins()).willReturn(1L);
        given(adminRepository.countDeletedAdmins()).willReturn(1L);
        given(adminRepository.findAllInactiveAdmins()).willReturn(inactiveAdmins);

        // inactive@test.com만 인증코드 없음
        given(redisVerificationCodeService.hasVerificationCode("pending@test.com")).willReturn(true);
        given(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).willReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "inactive", null, null);

        // then
        assertThat(result.admins()).hasSize(1);
        assertThat(result.admins().get(0).email()).isEqualTo("inactive@test.com");
        assertThat(result.admins().get(0).isActive()).isFalse();
    }

    @Test
    @DisplayName("퇴사 관리자만 조회")
    void getAdminList_DeletedOnly() {
        // given
        List<Admins> deletedAdmins = List.of(testAdmins.get(3));
        Page<Admins> adminPage = new PageImpl<>(deletedAdmins, pageable, deletedAdmins.size());

        given(adminRepository.findDeletedAdmins(pageable)).willReturn(adminPage);
        given(adminRepository.count()).willReturn(3L);
        given(adminRepository.countActiveAdmins()).willReturn(1L);
        given(adminRepository.countDeletedAdmins()).willReturn(1L);
        given(adminRepository.findAllInactiveAdmins()).willReturn(testAdmins.subList(1, 3));
        given(redisVerificationCodeService.hasVerificationCode("pending@test.com")).willReturn(true);
        given(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).willReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "deleted", null, null);

        // then
        assertThat(result.admins()).hasSize(1);
        assertThat(result.admins().get(0).email()).isEqualTo("deleted@test.com");
        assertThat(result.admins().get(0).isDeleted()).isTrue();
    }

    @Test
    @DisplayName("검색어로 관리자 조회")
    void getAdminList_WithSearch() {
        // given
        String searchTerm = "활성";
        List<Admins> searchResult = List.of(testAdmins.get(0));
        Page<Admins> adminPage = new PageImpl<>(searchResult, pageable, searchResult.size());

        given(adminRepository.findByNameOrEmailContainingIgnoreCase(searchTerm, pageable)).willReturn(adminPage);
        given(adminRepository.count()).willReturn(3L);
        given(adminRepository.countActiveAdmins()).willReturn(1L);
        given(adminRepository.countDeletedAdmins()).willReturn(1L);
        given(adminRepository.findAllInactiveAdmins()).willReturn(testAdmins.subList(1, 3));
        given(redisVerificationCodeService.hasVerificationCode("pending@test.com")).willReturn(true);
        given(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).willReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, null, searchTerm, null);

        // then
        assertThat(result.admins()).hasSize(1);
        assertThat(result.admins().get(0).name()).contains("활성");
        verify(adminRepository).findByNameOrEmailContainingIgnoreCase(searchTerm, pageable);
    }

    @Test
    @DisplayName("검색어 + 상태 필터 조합")
    void getAdminList_WithSearchAndStatus() {
        // given
        String searchTerm = "관리자";
        List<Admins> searchResult = List.of(testAdmins.get(0));
        Page<Admins> adminPage = new PageImpl<>(searchResult, pageable, searchResult.size());

        given(adminRepository.findActiveAdminsBySearch(searchTerm, pageable)).willReturn(adminPage);
        given(adminRepository.count()).willReturn(3L);
        given(adminRepository.countActiveAdmins()).willReturn(1L);
        given(adminRepository.countDeletedAdmins()).willReturn(1L);
        given(adminRepository.findAllInactiveAdmins()).willReturn(testAdmins.subList(1, 3));
        given(redisVerificationCodeService.hasVerificationCode("pending@test.com")).willReturn(true);
        given(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).willReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, "active", searchTerm, null);

        // then
        assertThat(result.admins()).hasSize(1);
        verify(adminRepository).findActiveAdminsBySearch(searchTerm, pageable);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("빈 검색어 처리")
    void getAdminList_EmptySearch(String emptySearch) {
        // given
        List<Admins> activeAdmins = testAdmins.subList(0, 3);
        Page<Admins> adminPage = new PageImpl<>(activeAdmins, pageable, activeAdmins.size());

        given(adminRepository.findAll(pageable)).willReturn(adminPage);
        given(adminRepository.count()).willReturn(3L);
        given(adminRepository.countActiveAdmins()).willReturn(1L);
        given(adminRepository.countDeletedAdmins()).willReturn(1L);
        given(adminRepository.findAllInactiveAdmins()).willReturn(testAdmins.subList(1, 3));
        given(redisVerificationCodeService.hasVerificationCode("pending@test.com")).willReturn(true);
        given(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).willReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, null, emptySearch, null);

        // then
        verify(adminRepository).findAll(pageable);
        verify(adminRepository, never()).findByNameOrEmailContainingIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("관리자 통계 계산")
    void calculateAdminStats() {
        // given
        List<Admins> inactiveAdmins = testAdmins.subList(1, 3);

        given(adminRepository.count()).willReturn(3L); // 전체 (퇴사자 제외)
        given(adminRepository.countActiveAdmins()).willReturn(1L); // 활성
        given(adminRepository.countDeletedAdmins()).willReturn(1L); // 퇴사
        given(adminRepository.findAllInactiveAdmins()).willReturn(inactiveAdmins); // 비활성 전체

        // Redis 상태 설정
        given(redisVerificationCodeService.hasVerificationCode("pending@test.com")).willReturn(true); // 대기
        given(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).willReturn(false); // 비활성

        // when
        AdminStatsDTO stats = adminManagementService.calculateAdminStats();

        // then
        assertThat(stats.totalCount()).isEqualTo(3L);
        assertThat(stats.activeCount()).isEqualTo(1L);
        assertThat(stats.pendingCount()).isEqualTo(1L); // Redis에 인증코드 있는 경우
        assertThat(stats.inactiveCount()).isEqualTo(1L); // Redis에 인증코드 없는 경우
        assertThat(stats.deletedCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Redis 인증코드 정보가 포함된 DTO 변환 - 대기 상태")
    void convertToDTO_WithPendingStatus() {
        // given
        Admins pendingAdmin = testAdmins.get(1); // pending@test.com
        List<Admins> admins = List.of(pendingAdmin);
        Page<Admins> adminPage = new PageImpl<>(admins, pageable, admins.size());

        given(adminRepository.findAll(pageable)).willReturn(adminPage);
        given(adminRepository.count()).willReturn(1L);
        given(adminRepository.countActiveAdmins()).willReturn(0L);
        given(adminRepository.countDeletedAdmins()).willReturn(0L);
        given(adminRepository.findAllInactiveAdmins()).willReturn(admins);

        // Redis 설정
        given(redisVerificationCodeService.hasVerificationCode("pending@test.com")).willReturn(true);
        given(redisVerificationCodeService.getVerificationCodeTTL("pending@test.com")).willReturn(3600L); // 1시간

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, null, null, null);

        // then
        AdminInfoResponseDTO adminInfo = result.admins().get(0);
        assertThat(adminInfo.email()).isEqualTo("pending@test.com");
        assertThat(adminInfo.verificationCode()).isEqualTo("EXISTS"); // 보안상 실제 코드는 반환하지 않음
        assertThat(adminInfo.verificationCodeExpiry()).isNotNull();
        assertThat(adminInfo.isActive()).isFalse();
    }

    @Test
    @DisplayName("Redis 인증코드 정보가 포함된 DTO 변환 - 활성 상태")
    void convertToDTO_WithActiveStatus() {
        // given
        Admins activeAdmin = testAdmins.get(0); // active@test.com
        List<Admins> admins = List.of(activeAdmin);
        Page<Admins> adminPage = new PageImpl<>(admins, pageable, admins.size());

        given(adminRepository.findAll(pageable)).willReturn(adminPage);
        given(adminRepository.count()).willReturn(1L);
        given(adminRepository.countActiveAdmins()).willReturn(1L);
        given(adminRepository.countDeletedAdmins()).willReturn(0L);
        given(adminRepository.findAllInactiveAdmins()).willReturn(List.of());

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(0, 10, null, null, null);

        // then
        AdminInfoResponseDTO adminInfo = result.admins().get(0);
        assertThat(adminInfo.email()).isEqualTo("active@test.com");
        assertThat(adminInfo.verificationCode()).isNull(); // 활성 계정은 인증코드 없음
        assertThat(adminInfo.verificationCodeExpiry()).isNull();
        assertThat(adminInfo.isActive()).isTrue();
    }

    @Test
    @DisplayName("페이지네이션 정보 검증")
    void getAdminList_PaginationInfo() {
        // given
        int page = 1;
        int size = 5;
        Pageable customPageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        List<Admins> activeAdmins = testAdmins.subList(0, 3);
        Page<Admins> adminPage = new PageImpl<>(activeAdmins, customPageable, 15L); // 총 15개 데이터

        given(adminRepository.findAll(customPageable)).willReturn(adminPage);
        given(adminRepository.count()).willReturn(3L);
        given(adminRepository.countActiveAdmins()).willReturn(1L);
        given(adminRepository.countDeletedAdmins()).willReturn(1L);
        given(adminRepository.findAllInactiveAdmins()).willReturn(testAdmins.subList(1, 3));
        given(redisVerificationCodeService.hasVerificationCode("pending@test.com")).willReturn(true);
        given(redisVerificationCodeService.hasVerificationCode("inactive@test.com")).willReturn(false);

        // when
        AdminListResponseDTO result = adminManagementService.getAdminList(page, size, null, null, null);

        // then
        assertThat(result.currentPage()).isEqualTo(page);
        assertThat(result.pageSize()).isEqualTo(size);
        assertThat(result.totalElements()).isEqualTo(15L);
        assertThat(result.totalPages()).isEqualTo(3); // 15 / 5 = 3
    }
}