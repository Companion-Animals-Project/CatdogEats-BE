package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 상품 매출 순위 DTO
 * 이번 달 상품별 매출 순위 데이터 (실시간 기준)
 */
@Schema(description = "상품 매출 순위")
public record ProductSalesRankingDTO(
        @Schema(description = "상품 ID", example = "prod-001")
        String productId,

        @Schema(description = "상품명", example = "강아지 수제 쿠키")
        String productName,

        @Schema(description = "총 판매 수량", example = "150")
        Long totalQuantity,

        @Schema(description = "총 매출액 (원)", example = "750000")
        Long totalSales
) { }