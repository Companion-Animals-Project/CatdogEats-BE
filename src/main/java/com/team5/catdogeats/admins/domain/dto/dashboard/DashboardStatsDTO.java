package com.team5.catdogeats.admins.domain.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private Long totalUsers;
    private Integer totalUsersPercentage;

    private Long monthlyNewUsers;
    private Integer monthlyNewUsersPercentage;

    private Long monthlyOrders;
    private Integer monthlyOrdersPercentage;

    private Long pendingReports;
}