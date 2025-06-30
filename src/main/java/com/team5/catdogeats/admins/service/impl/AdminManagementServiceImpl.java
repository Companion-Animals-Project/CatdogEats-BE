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

        Page<Admins> adminPage = getFilteredAdmins(pageable, status, search, department);

        List<AdminInfoResponseDTO> adminDTOs = adminPage.getContent().stream()
                .map(this::convertToDTO)
                .toList();

        AdminStatsDTO stats = calculateAdminStats();

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
        long activeCount = adminRepository.countByIsActiveTrue();
        long pendingCount = adminRepository.countByIsActiveFalseAndVerificationCodeIsNotNull();
        long inactiveCount = adminRepository.countByIsActiveFalse();

        return AdminStatsDTO.builder()
                .totalCount(totalCount)
                .activeCount(activeCount)
                .pendingCount(pendingCount)
                .inactiveCount(inactiveCount)
                .build();
    }

    private Page<Admins> getFilteredAdmins(Pageable pageable, String status, String search, Department department) {
        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean hasStatus = status != null && !status.equals("all");
        boolean hasDepartment = department != null;

        // 복합 조건 처리
        if (hasSearch && hasStatus) {
            return getAdminsBySearchAndStatus(search, status, pageable);
        }

        if (hasSearch && hasDepartment) {
            return adminRepository.findBySearchAndDepartment(search.trim(), department, pageable);
        }

        // 단일 조건 처리
        if (hasSearch) {
            return adminRepository.findByNameOrEmailContainingIgnoreCase(search.trim(), pageable);
        }

        if (hasStatus) {
            return getAdminsByStatus(status, pageable);
        }

        if (hasDepartment) {
            return adminRepository.findByDepartment(department, pageable);
        }

        // 조건 없음 - 전체 조회
        return adminRepository.findAll(pageable);
    }

    /**
     * 상태별 필터링
     */
    private Page<Admins> getAdminsByStatus(String status, Pageable pageable) {
        return switch (status.toLowerCase()) {
            case "active" -> adminRepository.findActiveAdmins(pageable);
            case "pending" -> adminRepository.findPendingAdmins(pageable);
            case "inactive" -> adminRepository.findInactiveAdmins(pageable);
            default -> adminRepository.findAll(pageable);
        };
    }



    /**
     * 검색어 + 상태 복합 조건
     */
    private Page<Admins> getAdminsBySearchAndStatus(String search, String status, Pageable pageable) {
        return switch (status.toLowerCase()) {
            case "active" -> adminRepository.findBySearchAndActiveStatus(search.trim(), true, pageable);
            case "inactive" -> adminRepository.findBySearchAndActiveStatus(search.trim(), false, pageable);
            case "pending" -> {
                // 대기중인 경우는 별도 처리 필요 (복잡한 조건)
                Page<Admins> allSearch = adminRepository.findByNameOrEmailContainingIgnoreCase(search.trim(), pageable);
                // 여기서는 간단히 전체 검색 결과를 반환하고, 필요시 별도 쿼리 추가
                yield allSearch;
            }
            default -> adminRepository.findByNameOrEmailContainingIgnoreCase(search.trim(), pageable);
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
