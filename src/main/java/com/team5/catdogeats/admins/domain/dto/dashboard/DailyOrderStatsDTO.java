// DailyOrderStatsDTO.java
package com.team5.catdogeats.admins.domain.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DailyOrderStatsDTO {
    private LocalDate orderDate;
    private Long customerCount;
    private Long orderCount;

    public DailyOrderStatsDTO(java.sql.Date orderDate, Long customerCount, Long orderCount) {
        this.orderDate = orderDate.toLocalDate();
        this.customerCount = customerCount;
        this.orderCount = orderCount;
    }
}