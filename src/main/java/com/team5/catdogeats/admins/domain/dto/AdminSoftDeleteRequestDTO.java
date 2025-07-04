package com.team5.catdogeats.admins.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSoftDeleteRequestDTO(
        @NotBlank @Email String targetEmail,
        @NotBlank @Email String requestedBy,
        @Size(max = 100, message = "퇴사 사유는 100자를 초과할 수 없습니다.")
        String reason
) {}