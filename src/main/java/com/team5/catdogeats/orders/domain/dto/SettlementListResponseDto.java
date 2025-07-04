package com.team5.catdogeats.orders.domain.dto;

import org.springframework.data.domain.Page;

/**
 * 정산 리스트 응답 DTO
 * 페이징된 정산 리스트와 요약 정보를 함께 반환
 */
public record SettlementListResponseDto(
        Page<SettlementItemDto> settlements,    // 페이징된 정산 리스트
        SettlementSummaryDto summary           // 정산 요약 정보
) {
}
