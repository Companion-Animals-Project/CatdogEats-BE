package com.team5.catdogeats.admins.domain.dto;

import lombok.Builder;

import java.time.ZonedDateTime;

@Builder
public record AdminSoftDeleteResponseDTO(
        String email,
        String name,
        ZonedDateTime deletedAt,
        String message
) {}
