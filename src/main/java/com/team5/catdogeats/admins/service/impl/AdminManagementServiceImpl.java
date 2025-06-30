package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminInfoResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminListResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminStatsDTO;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminManagementService;
import com.team5.catdogeats.admins.service.RedisVerificationCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Redis 기반 관리자 계정 관리 서비스 구현체
 * 인증코드 정보를 Redis에서 조회하여 표시
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminManagementServiceImpl implements AdminManagementService {

    private final AdminRepository adminRepository;
    private final RedisVerificationCodeService redisVerificationCodeService;

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

        // 복합 조건 처리 우선순위
        if (hasSearch && hasStatus) {
            adminPage = getAdminPageBySearchAndStatus(trimmedSearch, status, pageable);
            log.debug("검색어 + 상태 필터 적용: 검색어={}, 상태={}", trimmedSearch, status);

        } else if (hasSearch) {
            adminPage = adminRepository.findByNameOrEmailContainingIgnoreCase(trimmedSearch, pageable);
            log.debug("검색어만 적용: {}", trimmedSearch);

        } else if (hasStatus) {
            adminPage = getAdminPageByStatus(status, pageable);
            log.debug("상태 필터만 적용: {}", status);

        }  else {
            adminPage = adminRepository.findAll(pageable);
            log.debug("전체 조회");
        }

        // DTO 변환 (Redis 인증코드 정보 포함)
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

    @Override
    public AdminStatsDTO calculateAdminStats() {
        long totalCount = adminRepository.count();
        long activeCount = adminRepository.countActiveAdmins(); // isActive = true

        List<Admins> inactiveAdmins = adminRepository.findAllInactiveAdmins(); // isActive = false

        long pendingCount = 0;
        for (Admins admin : inactiveAdmins) {
            // Redis에서 인증코드 존재 여부만 확인
            if (redisVerificationCodeService.hasVerificationCode(admin.getEmail())) {
                pendingCount++;
            }
        }

        long inactiveCount = inactiveAdmins.size() - pendingCount;

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
     * 검색어 + 상태 조합으로 조회
     */
    private Page<Admins> getAdminPageBySearchAndStatus(String search, String status, Pageable pageable) {
        return switch (status.toLowerCase()) {
            case "active" -> adminRepository.findActiveAdminsBySearch(search, pageable);
            case "pending", "inactive" -> {
                // isActive = false인 계정들을 가져와서 서비스에서 Redis로 분류 (인증코드 있으면 pending, 없으면 inactive)
                Page<Admins> inactiveAdmins = adminRepository.findInactiveAdminsBySearch(search, pageable);
                yield filterByRedisStatus(inactiveAdmins, status);
            }
            default -> adminRepository.findByNameOrEmailContainingIgnoreCase(search, pageable);
        };
    }

    /**
     * 상태별로만 조회
     */
    private Page<Admins> getAdminPageByStatus(String status, Pageable pageable) {
        return switch (status.toLowerCase()) {
            case "active" -> adminRepository.findActiveAdmins(pageable);
            case "pending", "inactive" -> {
                Page<Admins> inactiveAdmins = adminRepository.findInactiveAdmins(pageable);
                yield filterByRedisStatus(inactiveAdmins, status);
            }
            default -> adminRepository.findAll(pageable);
        };
    }

    /**
     *
     */

    private Page<Admins> filterByRedisStatus(Page<Admins> inactiveAdmins, String targetStatus) {
        List<Admins> filtered = inactiveAdmins.getContent().stream()
                .filter(admin -> {
                    boolean hasCode = redisVerificationCodeService.hasVerificationCode(admin.getEmail());
                    if ("pending".equals(targetStatus)) {
                        return hasCode;    // 인증코드 있는 것만
                    } else {
                        return !hasCode;   // 인증코드 없는 것만
                    }
                })
                .toList();

        return new PageImpl<>(filtered, inactiveAdmins.getPageable(), filtered.size());
    }



    /**
     * Admin 엔티티를 DTO로 변환
     * Redis에서 인증코드 정보를 조회하여 포함
     */
    private AdminInfoResponseDTO convertToDTO(Admins admin) {
        // Redis에서 인증코드 정보 조회
        String verificationCode = null;
        ZonedDateTime verificationCodeExpiry = null;

        // 비활성 계정의 경우 Redis에서 인증코드 정보 확인
        if (!admin.getIsActive()) {
            boolean hasCode = redisVerificationCodeService.hasVerificationCode(admin.getEmail());
            if (hasCode) {
                long ttlSeconds = redisVerificationCodeService.getVerificationCodeTTL(admin.getEmail());
                if (ttlSeconds > 0) {
                    verificationCode = "EXISTS"; // 보안상 실제 코드는 반환하지 않음
                    verificationCodeExpiry = ZonedDateTime.now().plusSeconds(ttlSeconds);
                }
            }

            // 비밀번호 재설정 코드도 확인
            if (verificationCode == null) {
                boolean hasResetCode = redisVerificationCodeService.hasVerificationCode(admin.getEmail());
                if (hasResetCode) {
                    long ttlSeconds = redisVerificationCodeService.getVerificationCodeTTL(admin.getEmail());
                    if (ttlSeconds > 0) {
                        verificationCode = "RESET"; // 비밀번호 재설정 코드임을 표시
                        verificationCodeExpiry = ZonedDateTime.now().plusSeconds(ttlSeconds);
                    }
                }
            }
        }

        return AdminInfoResponseDTO.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .name(admin.getName())
                .department(admin.getDepartment())
                .isActive(admin.getIsActive())
                .isFirstLogin(admin.getIsFirstLogin())
                .verificationCode(verificationCode) // Redis에서 조회한 정보
                .verificationCodeExpiry(verificationCodeExpiry) // Redis TTL 기반 만료시간
                .lastLoginAt(admin.getUpdatedAt())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}