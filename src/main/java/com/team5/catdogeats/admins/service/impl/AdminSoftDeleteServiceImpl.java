package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminSoftDeleteRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminSoftDeleteResponseDTO;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminSoftDeleteService;
import com.team5.catdogeats.global.config.JpaTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSoftDeleteServiceImpl implements AdminSoftDeleteService {

    private final AdminRepository adminRepository;

    @Value("${admin.super.email}")
    private String superAdminEmail;

    @Override
    @JpaTransactional
    public AdminSoftDeleteResponseDTO softDeleteAdmin(AdminSoftDeleteRequestDTO request) {
        // 슈퍼관리자 보호
        if (superAdminEmail.equals(request.targetEmail())) {
            throw new IllegalArgumentException("슈퍼관리자 계정은 퇴사 처리할 수 없습니다.");
        }

        // 자기 자신 퇴사 방지
        if (request.targetEmail().equals(request.requestedBy())) {
            throw new IllegalArgumentException("자신의 계정은 퇴사 처리할 수 없습니다.");
        }

        Admins targetAdmin = adminRepository.findByEmail(request.targetEmail())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (targetAdmin.getIsDeleted()) {
            throw new IllegalStateException("이미 퇴사 처리된 관리자입니다.");
        }

        // 퇴사 사유와 함께 처리
        targetAdmin.softDelete(request.reason());
        adminRepository.save(targetAdmin);

        log.info("관리자 퇴사 처리: target={}, requestedBy={}, reason={}, deletedAt={}",
                request.targetEmail(), request.requestedBy(), request.reason(), targetAdmin.getDeletedAt());

        return AdminSoftDeleteResponseDTO.builder()
                .email(targetAdmin.getEmail())
                .name(targetAdmin.getName())
                .deletedAt(targetAdmin.getDeletedAt())
                .message("관리자가 성공적으로 퇴사 처리되었습니다.")
                .build();
    }

    @Override
    @JpaTransactional
    public AdminSoftDeleteResponseDTO undoSoftDelete(String adminEmail) {
        Admins admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if (!admin.getIsDeleted()) {
            throw new IllegalStateException("퇴사 처리되지 않은 관리자입니다.");
        }

        admin.undoSoftDelete();
        adminRepository.save(admin);

        log.info("관리자 퇴사 처리 취소: email={}", adminEmail);

        return AdminSoftDeleteResponseDTO.builder()
                .email(admin.getEmail())
                .name(admin.getName())
                .deletedAt(null)
                .message("관리자의 퇴사 처리가 취소되었습니다.")
                .build();
    }
}