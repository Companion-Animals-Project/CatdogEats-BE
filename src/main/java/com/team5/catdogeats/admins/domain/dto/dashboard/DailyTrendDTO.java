package com.team5.catdogeats.admins.domain.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrendDTO {
    private LocalDate date;
    private Long newUsers;
    private Long orderCount;
    private Long orderCustomers;
}