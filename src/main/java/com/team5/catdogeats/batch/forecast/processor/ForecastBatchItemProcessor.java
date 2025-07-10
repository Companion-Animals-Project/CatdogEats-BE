package com.team5.catdogeats.batch.forecast.processor;

import com.team5.catdogeats.batch.forecast.config.ForecastBatchProperties;
import com.team5.catdogeats.batch.forecast.domain.dto.ForecastBatchItem;
import com.team5.catdogeats.forecast.domain.dto.DemandForecastResultDTO;
import com.team5.catdogeats.forecast.service.DemandForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;

/**
 * 수요예측 배치 ItemProcessor (기존 서비스 로직 활용 버전)
 * DemandForecastService의 실제 비즈니스 로직을 호출하여 수요예측 실행
 */
@Slf4j
@RequiredArgsConstructor
public class ForecastBatchItemProcessor implements ItemProcessor<ForecastBatchItem, ForecastBatchItem> {

    private final DemandForecastService demandForecastService;
    private final ForecastBatchProperties forecastBatchProperties;

    /**
     * 판매자별 수요예측 처리
     * 기존 DemandForecastService.executeForecasting() 메서드를 활용
     */
    @Override
    public ForecastBatchItem process(ForecastBatchItem item) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            log.debug("수요예측 처리 시작 - sellerId: {}, vendorName: {}, 활성상품: {}개, 우선순위: {}",
                    item.sellerId(), item.vendorName(), item.activeProductCount(), item.getPriorityScore());

            // 1. 기본 유효성 검증
            if (!item.isValid()) {
                log.warn("유효하지 않은 아이템 - 스킵 처리: sellerId={}, vendorName={}",
                        item.sellerId(), item.vendorName());
                return item.withProcessingResult(
                        ForecastBatchItem.ProcessingResult.skipped("유효하지 않은 판매자 정보"));
            }

            // 2. 처리 대상 여부 확인
            if (!item.isEligibleForProcessing()) {
                log.debug("처리 대상이 아님 - sellerId: {}, 활성: {}, 상품수: {}",
                        item.sellerId(), item.isActive(), item.activeProductCount());
                return item.withProcessingResult(
                        ForecastBatchItem.ProcessingResult.skipped("비활성 판매자 또는 상품 없음"));
            }

            // 3. 최근 활동도 체크 및 우선순위 평가
            if (!item.hasRecentActivity() && item.getPriorityScore() < 30) {
                log.debug("우선순위 낮음 - sellerId: {}, 마지막주문: {}, 점수: {}",
                        item.sellerId(), item.lastOrderDate(), item.getPriorityScore());
                return item.withProcessingResult(
                        ForecastBatchItem.ProcessingResult.skipped("우선순위 낮음 (점수: " + item.getPriorityScore() + ")"));
            }

            // 4. 실제 수요예측 실행 (기존 서비스 로직 활용)
            log.info("수요예측 실행 중 - sellerId: {}, vendorName: {}", item.sellerId(), item.vendorName());

            int forecastedProductCount = demandForecastService.executeForecasting(item.sellerId());

            long processingTime = System.currentTimeMillis() - startTime;

            // 5. 예측 결과가 0개인 경우 처리
            if (forecastedProductCount == 0) {
                log.info("예측 가능한 상품 없음 - sellerId: {}, 처리시간: {}ms",
                        item.sellerId(), processingTime);
                return item.withProcessingResult(
                        ForecastBatchItem.ProcessingResult.skipped("예측 가능한 상품 없음 (데이터 부족)"));
            }

            // 6. 예측 결과 상세 분석 (기존 서비스에서 결과 조회)
            ForecastAnalysisResult analysisResult = analyzeForcastResults(item.sellerId(), forecastedProductCount);

            // 7. 성공 결과 생성
            ForecastBatchItem.ProcessingResult result = ForecastBatchItem.ProcessingResult.success(
                    forecastedProductCount,
                    processingTime,
                    analysisResult.averageConfidenceScore(),
                    analysisResult.shortageProductCount()
            );

            log.info("수요예측 처리 완료 - sellerId: {}, 처리상품: {}개, 신뢰도: {:.2f}, 재고부족: {}개, 소요시간: {}ms",
                    item.sellerId(), forecastedProductCount,
                    analysisResult.averageConfidenceScore(), analysisResult.shortageProductCount(), processingTime);

            return item.withProcessingResult(result);

        } catch (IllegalArgumentException e) {
            // 비즈니스 로직 검증 실패 (스킵 처리)
            long processingTime = System.currentTimeMillis() - startTime;
            log.warn("수요예측 비즈니스 검증 실패 - sellerId: {}, 사유: {}, 소요시간: {}ms",
                    item.sellerId(), e.getMessage(), processingTime);

            return item.withProcessingResult(
                    ForecastBatchItem.ProcessingResult.skipped("비즈니스 검증 실패: " + e.getMessage()));

        } catch (Exception e) {
            // 예상치 못한 오류 (실패 처리)
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("수요예측 처리 실패 - sellerId: {}, vendorName: {}, error: {}, 소요시간: {}ms",
                    item.sellerId(), item.vendorName(), e.getMessage(), processingTime, e);

            return item.withProcessingResult(
                    ForecastBatchItem.ProcessingResult.failure(e.getMessage(), processingTime));
        }
    }

    /**
     * 예측 결과 상세 분석
     * 기존 DemandForecastService의 getLatestForecastResults()를 활용하여 실제 결과 분석
     */
    private ForecastAnalysisResult analyzeForcastResults(String sellerId, int forecastedProductCount) {
        try {
            // 기존 서비스에서 최신 예측 결과 조회
            List<DemandForecastResultDTO> forecastResults = demandForecastService.getLatestForecastResults(sellerId);

            if (forecastResults.isEmpty()) {
                log.warn("예측 결과 조회 실패 - sellerId: {}, 기본값 사용", sellerId);
                return new ForecastAnalysisResult(
                        calculateDefaultConfidenceScore(forecastedProductCount),
                        0
                );
            }

            // 신뢰도 점수 평균 계산
            double averageConfidence = forecastResults.stream()
                    .mapToDouble(DemandForecastResultDTO::confidenceScore)
                    .average()
                    .orElse(0.7);

            // 재고 부족 상품 수 계산
            int shortageCount = (int) forecastResults.stream()
                    .mapToInt(DemandForecastResultDTO::shortageQuantity)
                    .filter(shortage -> shortage > 0)
                    .count();

            // 낮은 신뢰도 경고
            if (averageConfidence < forecastBatchProperties.getNotification().getLowConfidenceThreshold()) {
                log.warn("낮은 신뢰도 감지 - sellerId: {}, 평균신뢰도: {:.2f}, 임계값: {:.2f}",
                        sellerId, averageConfidence,
                        forecastBatchProperties.getNotification().getLowConfidenceThreshold());
            }

            // 높은 재고 부족 경고
            if (shortageCount > 0) {
                log.warn("재고 부족 상품 감지 - sellerId: {}, 부족상품수: {}개", sellerId, shortageCount);
            }

            return new ForecastAnalysisResult(averageConfidence, shortageCount);

        } catch (Exception e) {
            log.error("예측 결과 분석 실패 - sellerId: {}, 기본값 사용", sellerId, e);
            return new ForecastAnalysisResult(
                    calculateDefaultConfidenceScore(forecastedProductCount),
                    0
            );
        }
    }

    /**
     * 기본 신뢰도 점수 계산 (예측 결과 조회 실패 시 사용)
     * 예측된 상품 수를 기반으로 한 휴리스틱
     */
    private double calculateDefaultConfidenceScore(int forecastedProductCount) {
        if (forecastedProductCount >= 20) {
            return 0.9; // 많은 상품을 예측했으면 높은 신뢰도
        } else if (forecastedProductCount >= 10) {
            return 0.8;
        } else if (forecastedProductCount >= 5) {
            return 0.7;
        } else {
            return 0.6; // 적은 상품 예측은 낮은 신뢰도
        }
    }

    /**
     * 예측 분석 결과 Record
     */
    private record ForecastAnalysisResult(
            double averageConfidenceScore,
            int shortageProductCount
    ) {
        /**
         * 분석 결과 유효성 검증
         */
        public boolean isValid() {
            return averageConfidenceScore >= 0.0 && averageConfidenceScore <= 1.0
                    && shortageProductCount >= 0;
        }

        /**
         * 고품질 예측 여부 (신뢰도 0.8 이상)
         */
        public boolean isHighQuality() {
            return averageConfidenceScore >= 0.8;
        }

        /**
         * 재고 부족 위험 여부
         */
        public boolean hasShortageRisk() {
            return shortageProductCount > 0;
        }
    }
}