package com.team5.catdogeats.users.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 판매자 대시보드 응답 DTO
 * 판매자 대시보드에 표시할 모든 데이터를 포함하는 최종 응답 DTO
 */
@Schema(description = "판매자 대시보드 응답")
public record SellerDashboardResponseDTO(
        @Schema(description = "오늘 주문 통계")
        TodayStatsDTO todayStats,

        @Schema(description = "주간 매출 동향 (7일)", example = "[{\"salesDate\":\"2025-01-15\",\"dailySales\":450000}]")
        List<WeeklySalesDTO> weeklySales,

        @Schema(description = "이번 달 상품 매출 순위 (최대 10개)", example = "[{\"productId\":\"prod-001\",\"productName\":\"강아지 쿠키\",\"totalQuantity\":150,\"totalSales\":750000}]")
        List<ProductSalesRankingDTO> productRanking,

        @Schema(description = "데이터 조회 시각", example = "2025-01-15T14:30:00")
        String dataRetrievedAt
) {
    /**
     * 현재 시각을 포함하여 SellerDashboardResponseDTO 생성
     */
    public static SellerDashboardResponseDTO of(
            TodayStatsDTO todayStats,
            List<WeeklySalesDTO> weeklySales,
            List<ProductSalesRankingDTO> productRanking) {

        return new SellerDashboardResponseDTO(
                todayStats != null ? todayStats : TodayStatsDTO.empty(),
                weeklySales != null ? weeklySales : List.of(),
                productRanking != null ? productRanking : List.of(),
                ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        );
    }

}