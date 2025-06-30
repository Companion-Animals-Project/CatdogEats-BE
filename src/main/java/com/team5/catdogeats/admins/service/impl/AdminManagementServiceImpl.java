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
        log.debug("관리자 목록 조회 요청: page={}, size={}, status={}, search={}, department={}",
                page, size, status, search, department);

        // 페이지네이션 설정 (정렬은 쿼리에서 처리)
        Pageable pageable = PageRequest.of(page, size);

        Page<Admins> adminPage = getFilteredAdmins(pageable, status, search, department);

        List<AdminInfoResponseDTO> adminDTOs = adminPage.getContent().stream()
                .map(this::convertToDTO)
                .toList();

        AdminStatsDTO stats = calculateAdminStats();

        log.debug("관리자 목록 조회 완료: totalElements={}, totalPages={}",
                adminPage.getTotalElements(), adminPage.getTotalPages());

        return AdminListResponseDTO.builder()
                .admins(adminDTOs)
                .totalElements(adminPage.getTotalElements())
                .totalPages(adminPage.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .stats(stats)
                .build();
    }

    @Override
    public AdminStatsDTO calculateAdminStats() {
        long totalCount = adminRepository.count();
        long activeCount = adminRepository.countActiveAdmins();
        long pendingCount = adminRepository.countPendingAdmins();
        long inactiveCount = adminRepository.countInactiveAdmins();

        log.debug("관리자 통계: total={}, active={}, pending={}, inactive={}",
                totalCount, activeCount, pendingCount, inactiveCount);

        return AdminStatsDTO.builder()
                .totalCount(totalCount)
                .activeCount(activeCount)
                .pendingCount(pendingCount)
                .inactiveCount(inactiveCount)
                .build();
    }

    /**
     * 필터링 조건에 따른 관리자 목록 조회
     */
    private Page<Admins> getFilteredAdmins(Pageable pageable, String status, String search, Department department) {
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStatus = status != null && !status.equals("all");
        boolean hasDepartment = department != null;

        log.debug("필터링 조건: hasSearch={}, hasStatus={}, hasDepartment={}", hasSearch, hasStatus, hasDepartment);


        // 1. 검색어만
        if (hasSearch) {
            return adminRepository.findByNameOrEmailContainingIgnoreCase(search.trim(), pageable);
        }

        // 2. 상태만
        if (hasStatus) {
            return getAdminsByStatus(status, pageable);
        }

        // 3. 조건 없음 - 전체 조회 (최신순)
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by("createdAt").descending());
        return adminRepository.findAll(sortedPageable);
    }

    /**
     * 상태별 필터링
     */
    private Page<Admins> getAdminsByStatus(String status, Pageable pageable) {
        return switch (status.toLowerCase()) {
            case "active" -> {
                log.debug("활성 관리자 조회");
                yield adminRepository.findActiveAdmins(pageable);
            }
            case "pending" -> {
                log.debug("대기중 관리자 조회");
                yield adminRepository.findPendingAdmins(pageable);
            }
            case "inactive" -> {
                log.debug("비활성 관리자 조회");
                yield adminRepository.findInactiveAdmins(pageable);
            }
            default -> {
                log.debug("전체 관리자 조회 (unknown status: {})", status);
                Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                        Sort.by("createdAt").descending());
                yield adminRepository.findAll(sortedPageable);
            }
        };
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
                .lastLoginAt(admin.getLastLoginAt())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}