package com.team5.catdogeats.orders.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "상품별 매출 데이터")
public record ProductSalesDataDTO(
        @Schema(description = "상품 ID", example = "prod-001")
        String productId,

        @Schema(description = "상품 이름", example = "강아지 수제 쿠키")
        String productName,

        @Schema(description = "상품별 총 매출액", example = "2500000")
        Long totalAmount,

        @Schema(description = "상품별 총 판매수량", example = "500")
        Long quantity,

        @Schema(description = "전체 매출 대비 퍼센트", example = "20.5")
        Double percentage
) {}
