package com.team5.catdogeats.orders.domain.dto;

import org.springframework.data.domain.Page;

import java.time.YearMonth;

/**
 * 월별 정산내역 영수증 응답 DTO (페이징 포함)
 */
public record MonthlySettlementReceiptDto(
        YearMonth targetMonth,              // 대상 월
        String vendorName,                  // 업체명
        String businessNumber,              // 사업자번호
        Page<SettlementItemDTO> items,      // 페이징된 정산 아이템 리스트
        MonthlySettlementStatusDto summary  // 월별 정산 요약 정보
) {
}