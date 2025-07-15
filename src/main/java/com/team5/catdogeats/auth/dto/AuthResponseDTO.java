package com.team5.catdogeats.auth.dto;

import com.team5.catdogeats.users.domain.enums.Role;

public record AuthResponseDTO(String name, Role role) {
}
