package com.team5.catdogeats.batch.forecast.processor;

import com.team5.catdogeats.batch.forecast.config.ForecastBatchProperties;
import com.team5.catdogeats.batch.forecast.dto.ForecastBatchItem;
import com.team5.catdogeats.forecast.service.DemandForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

/**
 * 수요예측 배치 ItemProcessor
 * 각 판매자에 대해 수요예측 실행
 */
@Slf4j
@RequiredArgsConstructor
public class ForecastBatchItemProcessor implements ItemProcessor<ForecastBatchItem, ForecastBatchItem> {

    private final DemandForecastService demandForecastService;
    private final ForecastBatchProperties forecastBatchProperties;

    /**
     * 판매자별 수요예측 처리
     */
    @Override
    public ForecastBatchItem process(ForecastBatchItem item) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("수요예측 처리 시작 - sellerId: {}, vendorName: {}, 활성상품: {}개",
                    item.getSellerId(), item.getVendorName(), item.getActiveProductCount());

            // 1. 기본 검증
            if (!item.isValid()) {
                log.warn("유효하지 않은 아이템 - 스킵 처리: {}", item.getSellerId());
                item.setProcessingResult(
                        ForecastBatchItem.ProcessingResult.skipped("유효하지 않은 판매자 정보"));
                return item;
            }

            // 2. 처리 대상 여부 확인
            if (!item.isEligibleForProcessing()) {
                log.debug("처리 대상이 아님 - sellerId: {}, 활성: {}, 상품수: {}",
                        item.getSellerId(), item.isActive(), item.getActiveProductCount());
                item.setProcessingResult(
                        ForecastBatchItem.ProcessingResult.skipped("비활성 판매자 또는 상품 없음"));
                return item;
            }

            // 3. 최근 활동도 체크 (선택적)
            if (!item.hasRecentActivity()) {
                log.debug("최근 활동 없음 - sellerId: {}, 마지막주문: {}",
                        item.getSellerId(), item.getLastOrderDate());

                // 활동이 없어도 처리는 하되, 우선순위를 낮춤
                if (item.getPriorityScore() < 30) {
                    item.setProcessingResult(
                            ForecastBatchItem.ProcessingResult.skipped("최근 활동 부족"));
                    return item;
                }
            }

            // 4. 실제 수요예측 실행
            int forecastedProductCount = demandForecastService.executeForecasting(item.getSellerId());

            long processingTime = System.currentTimeMillis() - startTime;

            // 5. 예측 결과 분석 (간단한 통계)
            ProcessingStatistics stats = analyzeResults(item.getSellerId(), forecastedProductCount);

            // 6. 처리 결과 설정
            item.setProcessingResult(
                    ForecastBatchItem.ProcessingResult.success(
                            forecastedProductCount,
                            processingTime,
                            stats.averageConfidenceScore,
                            stats.shortageProductCount
                    ));

            log.info("수요예측 처리 완료 - sellerId: {}, 예측상품: {}개, 부족상품: {}개, 소요시간: {}ms",
                    item.getSellerId(), forecastedProductCount, stats.shortageProductCount, processingTime);

            return item;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;

            log.error("수요예측 처리 실패 - sellerId: {}, 소요시간: {}ms",
                    item.getSellerId(), processingTime, e);

            // 실패 결과 설정
            item.setProcessingResult(
                    ForecastBatchItem.ProcessingResult.failure(
                            "예측 실행 실패: " + e.getMessage(), processingTime));

            // null을 반환하면 Writer로 전달되지 않으므로, 실패 정보도 기록하기 위해 아이템 반환
            return item;
        }
    }

    /**
     * 처리 결과 분석
     */
    private ProcessingStatistics analyzeResults(String sellerId, int forecastedProductCount) {
        try {
            if (forecastedProductCount == 0) {
                return ProcessingStatistics.empty();
            }

            // 최신 예측 결과 조회해서 통계 계산
            var forecastResults = demandForecastService.getLatestForecastResults(sellerId);

            if (forecastResults.isEmpty()) {
                return ProcessingStatistics.empty();
            }

            // 신뢰도 점수 평균 계산
            double averageConfidence = forecastResults.stream()
                    .mapToDouble(result -> result.confidenceScore() != null ? result.confidenceScore() : 0.0)
                    .average()
                    .orElse(0.0);

            // 재고 부족 상품 수 계산
            int shortageCount = (int) forecastResults.stream()
                    .filter(result -> result.shortageQuantity() > 0)
                    .count();

            return ProcessingStatistics.builder()
                    .averageConfidenceScore(averageConfidence)
                    .shortageProductCount(shortageCount)
                    .totalForecastedProducts(forecastResults.size())
                    .build();

        } catch (Exception e) {
            log.warn("예측 결과 분석 실패 - sellerId: {}", sellerId, e);
            return ProcessingStatistics.empty();
        }
    }

    /**
     * 처리 통계 정보
     */
    @lombok.Builder
    @lombok.Getter
    private static class ProcessingStatistics {
        private final Double averageConfidenceScore;
        private final int shortageProductCount;
        private final int totalForecastedProducts;

        public static ProcessingStatistics empty() {
            return ProcessingStatistics.builder()
                    .averageConfidenceScore(0.0)
                    .shortageProductCount(0)
                    .totalForecastedProducts(0)
                    .build();
        }
    }

    /**
     * 프로세서 성능 모니터링용 메서드
     */
    public ProcessorMetrics getProcessorMetrics() {
        // 실제 운영에서는 Micrometer 등을 사용하여 메트릭 수집
        return ProcessorMetrics.builder()
                .processingTimeThreshold(forecastBatchProperties.getNotification().getSlowProcessingThreshold())
                .confidenceThreshold(forecastBatchProperties.getNotification().getLowConfidenceThreshold())
                .build();
    }

    /**
     * 프로세서 메트릭 정보
     */
    @lombok.Builder
    @lombok.Getter
    public static class ProcessorMetrics {
        private final long processingTimeThreshold;
        private final double confidenceThreshold;
    }
}