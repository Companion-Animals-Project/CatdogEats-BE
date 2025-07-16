package com.team5.catdogeats.forecast.domain.dto;

import com.team5.catdogeats.forecast.domain.DemandForecasts;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "수요예측 결과 (재고 부족량 포함)")
public record DemandForecastResultDTO(
        @Schema(description = "예측 ID") String id,
        @Schema(description = "판매자 ID") String sellerId,
        @Schema(description = "상품 ID") String productId,
        @Schema(description = "상품명") String productName,
        @Schema(description = "현재 재고량") Integer currentStock,
        @Schema(description = "7일간 예상 판매량") Integer predictedQuantity,
        @Schema(description = "부족 예상량 (0이면 충분)") Integer shortageQuantity,
        @Schema(description = "예측 알고리즘") String algorithmType,
        @Schema(description = "신뢰도 점수") Double confidenceScore,
        @Schema(description = "예측 생성일") LocalDate forecastDate
) {
}
