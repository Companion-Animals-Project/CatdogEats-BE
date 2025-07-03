package com.team5.catdogeats.admins.domain.dto;

import com.team5.catdogeats.admins.domain.enums.Department;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * 관리자 정보 응답 DTO
 */
@Data
@Builder
public class AdminSessionInfo {
    private String adminId;
    private String email;
    private String name;
    private Department department;
    private boolean isFirstLogin;
    private ZonedDateTime loginTime;

    public boolean isValid() {
        return adminId != null && email != null && name != null;
    }
}