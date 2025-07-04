package com.team5.catdogeats.orders.domain.dto;

/**
 * 이번달 정산현황 DTO
 */
public record MonthlySettlementStatusDto(
        Long totalMonthlyAmount,      // 이번달 총 정산금액 (확정+예정)
        Long confirmedAmount,         // 정산확정금액 (완료+처리중)
        Long pendingAmount           // 정산예정금액 (대기중)
) {
}
