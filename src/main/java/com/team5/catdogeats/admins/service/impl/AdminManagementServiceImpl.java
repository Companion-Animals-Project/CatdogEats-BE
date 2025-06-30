package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminInfoResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminListResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminStatsDTO;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 관리자 계정 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminManagementServiceImpl implements AdminManagementService {

    private final AdminRepository adminRepository;

    @Override
    public AdminListResponseDTO getAdminList(int page, int size, String status, String search, Department department) {
        // 페이지네이션 설정
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 검색어 정리
        boolean hasSearch = search != null && !search.trim().isEmpty();
        String trimmedSearch = hasSearch ? search.trim() : null;

        // 상태 정리
        boolean hasStatus = status != null && !status.equals("all");

        log.debug("검색 조건 - 검색어: {}, 상태: {}, 부서: {}", trimmedSearch, status, department);

        Page<Admins> adminPage;

        // 복합 조건 처리 우선순위:
        // 1. 검색어 + 상태
        // 2. 검색어 + 부서
        // 3. 검색어만
        // 4. 상태만
        // 5. 부서만
        // 6. 전체

        if (hasSearch && hasStatus) {
            // 검색어 + 상태 조합
            adminPage = getAdminPageBySearchAndStatus(trimmedSearch, status, pageable);
            log.debug("검색어 + 상태 필터 적용: 검색어={}, 상태={}", trimmedSearch, status);

        } else if (hasSearch && department != null) {
            // 검색어 + 부서 조합
            adminPage = adminRepository.findByDepartmentAndSearch(department, trimmedSearch, pageable);
            log.debug("검색어 + 부서 필터 적용: 검색어={}, 부서={}", trimmedSearch, department);

        } else if (hasSearch) {
            // 검색어만
            adminPage = adminRepository.findByNameOrEmailContainingIgnoreCase(trimmedSearch, pageable);
            log.debug("검색어만 적용: {}", trimmedSearch);

        } else if (hasStatus) {
            // 상태만
            adminPage = getAdminPageByStatus(status, pageable);
            log.debug("상태 필터만 적용: {}", status);

        } else if (department != null) {
            // 부서만 (메서드명 변경)
            adminPage = adminRepository.findByDepartmentWithPagination(department, pageable);
            log.debug("부서 필터만 적용: {}", department);

        } else {
            // 전체 조회
            adminPage = adminRepository.findAll(pageable);
            log.debug("전체 조회");
        }

        // DTO 변환
        List<AdminInfoResponseDTO> adminDTOs = adminPage.getContent().stream()
                .map(this::convertToDTO)
                .toList();

        // 통계 정보 계산
        AdminStatsDTO stats = calculateAdminStats();

        log.debug("조회 결과 - 총 {}건, 현재 페이지: {}/{}",
                adminPage.getTotalElements(), page + 1, adminPage.getTotalPages());

        return AdminListResponseDTO.builder()
                .admins(adminDTOs)
                .totalElements(adminPage.getTotalElements())
                .totalPages(adminPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .stats(stats)
                .build();
    }

    /**
     * 검색어 + 상태 조합으로 조회
     */
    private Page<Admins> getAdminPageBySearchAndStatus(String search, String status, Pageable pageable) {
        return switch (status.toLowerCase()) {
            case "active" -> adminRepository.findActiveAdminsBySearch(search, pageable);
            case "pending" -> adminRepository.findPendingAdminsBySearch(search, pageable);
            case "inactive" -> adminRepository.findInactiveAdminsBySearch(search, pageable);
            default -> adminRepository.findByNameOrEmailContainingIgnoreCase(search, pageable);
        };
    }

    /**
     * 상태별로만 조회
     */
    private Page<Admins> getAdminPageByStatus(String status, Pageable pageable) {
        return switch (status.toLowerCase()) {
            case "active" -> adminRepository.findActiveAdmins(pageable);
            case "pending" -> adminRepository.findPendingAdmins(pageable);
            case "inactive" -> adminRepository.findInactiveAdmins(pageable);
            default -> adminRepository.findAll(pageable);
        };
    }

    @Override
    public AdminStatsDTO calculateAdminStats() {
        long totalCount = adminRepository.count();
        long activeCount = adminRepository.countActiveAdmins();
        long pendingCount = adminRepository.countPendingAdmins();
        long inactiveCount = adminRepository.countInactiveAdmins();

        log.debug("통계 - 전체: {}, 활성: {}, 대기: {}, 비활성: {}",
                totalCount, activeCount, pendingCount, inactiveCount);

        return AdminStatsDTO.builder()
                .totalCount(totalCount)
                .activeCount(activeCount)
                .pendingCount(pendingCount)
                .inactiveCount(inactiveCount)
                .build();
    }

    /**
     * Admin 엔티티를 DTO로 변환
     */
    private AdminInfoResponseDTO convertToDTO(Admins admin) {
        return AdminInfoResponseDTO.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .name(admin.getName())
                .department(admin.getDepartment())
                .isActive(admin.getIsActive())
                .isFirstLogin(admin.getIsFirstLogin())
                .verificationCode(admin.getVerificationCode())
                .verificationCodeExpiry(admin.getVerificationCodeExpiry())
                .lastLoginAt(admin.getUpdatedAt())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}