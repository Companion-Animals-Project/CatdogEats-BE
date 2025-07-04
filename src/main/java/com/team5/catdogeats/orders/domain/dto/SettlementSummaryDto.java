package com.team5.catdogeats.orders.domain.dto;

/**
 * 정산 요약 정보 DTO
 * 총 정산 건수, 금액, 상태별 금액 등을 담는 Record
 */
public record SettlementSummaryDto(
        Long totalCount,                    // 총 정산내역 건수
        Long totalSettlementAmount,         // 총 정산금액
        Long completedAmount,               // 정산완료 금액 총합
        Long pendingAmount,                 // 대기중+처리중 금액 총합
        Long averageSettlementAmount       // 평균 정산금액
) {
}
