package com.team5.catdogeats.admins.domain.dto;

import com.team5.catdogeats.admins.domain.enums.Department;
import lombok.Builder;
import lombok.Data;

import java.time.ZonedDateTime;

/**
 * 관리자 정보 응답 DTO
 */
public record AdminInfo(
        String adminId,
        String email,
        String name,
        Department department,
        boolean isFirstLogin,
        ZonedDateTime loginTime
) {

    /**
     * 유효성 검증
     */
    public boolean isValid() {
        return adminId != null && email != null && name != null;
    }



}