package com.team5.catdogeats.admins.domain.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyUserStatsDTO {
    private String month;
    private Long userCount;
}