package com.team5.catdogeats.orders.domain.dto;

/**
 * 이번달 정산현황 DTO
 */
public record MonthlySettlementStatusDto(
        /**
         * 총 정산 건수
         */
        Long totalCount,

        /**
         * 이번달 총 정산금액
         * completedAmount + inProgressAmount 합계
         */
        Long totalMonthlyAmount,

        /**
         * 정산 완료 건수
         */
        Long completedCount,

        /**
         * 정산 완료 금액
         * Settlement 테이블의 COMPLETED 상태만
         */
        Long completedAmount,

        /**
         * 정산 처리중 건수
         */
        Long inProgressCount,

        /**
         * 정산 처리중 금액
         * Settlement 테이블의 IN_PROGRESS 상태만
         */
        Long inProgressAmount
) {}
