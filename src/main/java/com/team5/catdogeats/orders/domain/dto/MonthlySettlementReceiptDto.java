package com.team5.catdogeats.orders.domain.dto;

import java.time.YearMonth;
import java.util.List;

/**
 * 월별 정산내역 영수증 DTO
 */
public record MonthlySettlementReceiptDto(
        YearMonth targetMonth,              // 대상 월
        String sellerName,                  // 판매자명
        String businessNumber,              // 사업자등록번호
        List<SettlementItemDto> items,      // 정산 아이템 목록
        SettlementSummaryDto summary        // 월별 정산 요약
) {
}