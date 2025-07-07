package com.team5.catdogeats.orders.domain.dto;

import java.time.YearMonth;
import java.util.List;

/**
 * 월별 정산내역 영수증 DTO
 * CSV Export용 데이터 구조
 */
public record MonthlySettlementReceiptDto(
        /**
         * 대상 년월
         */
        YearMonth targetMonth,

        /**
         * 판매자명 (상호명)
         */
        String vendorName,

        /**
         * 사업자번호
         */
        String businessNumber,

        /**
         * 월별 정산 아이템 리스트
         */
        List<SettlementItemDto> items,

        /**
         * 월별 정산 요약 정보 (개수와 금액 모두 포함)
         */
        MonthlySettlementStatusDto summary
) {}