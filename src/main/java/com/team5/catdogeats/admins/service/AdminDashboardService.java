package com.team5.catdogeats.admins.service;

import com.team5.catdogeats.admins.domain.dto.dashboard.DashboardResponseDTO;

public interface AdminDashboardService {
    /**
     * 대시보드 전체 데이터 조회
     * @return 대시보드 응답 DTO
     */
    DashboardResponseDTO getDashboardData();
}