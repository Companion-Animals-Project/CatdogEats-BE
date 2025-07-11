package com.team5.catdogeats.orders.domain.dto;

/**
 * 정산 요약 정보 DTO
 * 총 정산 건수, 금액, 상태별 금액 등을 담는 Record
 */
public record SettlementSummaryDTO(
        /**
         * 총 정산 건수
         * Settlement 테이블의 모든 데이터
         */
        Long totalCount,

        /**
         * 총 정산 금액
         * completedAmount + inProgressAmount 합계
         */
        Long totalSettlementAmount,

        /**
         * 정산 완료 금액
         * Settlement 테이블의 COMPLETED 상태만
         */
        Long completedAmount,

        /**
         * 정산 처리중 금액
         * Settlement 테이블의 IN_PROGRESS 상태만
         */
        Long inProgressAmount
) {}
