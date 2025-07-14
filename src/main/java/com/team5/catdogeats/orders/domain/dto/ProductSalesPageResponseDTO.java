package com.team5.catdogeats.orders.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "상품별 매출 페이징 응답")
public record ProductSalesPageResponseDTO(
        @Schema(description = "상품별 매출 데이터 목록")
        List<ProductSalesDataDTO> content,

        @Schema(description = "전체 상품 개수", example = "25")
        Long totalElements,

        @Schema(description = "페이지 크기", example = "8")
        Integer size,

        @Schema(description = "현재 페이지 번호", example = "0")
        Integer number,

        @Schema(description = "전체 페이지 수", example = "4")
        Integer totalPages,

        @Schema(description = "첫 번째 페이지 여부", example = "true")
        Boolean first,

        @Schema(description = "마지막 페이지 여부", example = "false")
        Boolean last
) {
    /**
     * 빈 페이징 응답 생성
     */
    public static ProductSalesPageResponseDTO empty() {
        return new ProductSalesPageResponseDTO(
                List.of(), 0L, 8, 0, 0, true, true
        );
    }
}
