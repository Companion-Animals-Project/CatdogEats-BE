package com.team5.catdogeats.admins.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminRestoreRequestDTO(
        @NotBlank @Email String email
) {}