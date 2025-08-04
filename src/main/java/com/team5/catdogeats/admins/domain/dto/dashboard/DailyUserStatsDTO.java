package com.team5.catdogeats.admins.domain.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DailyUserStatsDTO {
    private LocalDate joinDate;
    private Long userCount;

    public DailyUserStatsDTO(java.sql.Date joinDate, Long userCount) {
        this.joinDate = joinDate.toLocalDate();
        this.userCount = userCount;
    }
}