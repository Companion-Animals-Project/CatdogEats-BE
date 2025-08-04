package com.team5.catdogeats.admins.domain.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponseDTO {
    private DashboardStatsDTO stats;
    private List<MonthlyUserStatsDTO> monthlyUserStats;
    private List<DailyTrendDTO> dailyTrends;
}