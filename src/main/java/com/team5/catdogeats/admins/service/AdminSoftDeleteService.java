package com.team5.catdogeats.admins.service;

import com.team5.catdogeats.admins.domain.dto.AdminSoftDeleteRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminSoftDeleteResponseDTO;

public interface AdminSoftDeleteService {
    AdminSoftDeleteResponseDTO softDeleteAdmin(AdminSoftDeleteRequestDTO request);
    AdminSoftDeleteResponseDTO undoSoftDelete(String adminEmail);
}
