package com.team5.catdogeats.batch.writer;

import com.team5.catdogeats.batch.dto.ForecastBatchItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 수요예측 배치 ItemWriter (기존 서비스 로직 연동 버전)
 * 실제 DemandForecastService 처리 결과를 기반으로 통계 수집 및 로깅
 */
@Slf4j
@RequiredArgsConstructor
public class ForecastBatchItemWriter implements ItemWriter<ForecastBatchItem> {

    // 배치 통계
    private final AtomicInteger totalProcessedCount = new AtomicInteger(0);
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger skippedCount = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private final AtomicInteger totalForecastedProducts = new AtomicInteger(0);
    private final AtomicInteger totalShortageProducts = new AtomicInteger(0);
    private final AtomicReference<Double> confidenceScoreSum = new AtomicReference<>(0.0);
    private final AtomicInteger confidenceScoreCount = new AtomicInteger(0);

    // 품질 분석 통계
    private final AtomicInteger highQualityPredictions = new AtomicInteger(0); // 신뢰도 0.8 이상
    private final AtomicInteger lowQualityPredictions = new AtomicInteger(0);  // 신뢰도 0.5 미만
    private final AtomicInteger highShortageVendors = new AtomicInteger(0);    // 재고 부족 5개 이상

    /**
     * 청크 단위로 처리 결과 기록
     * 실제 수요예측 서비스 처리 결과를 기반으로 상세 통계 수집
     */
    @Override
    public void write(Chunk<? extends ForecastBatchItem> chunk) throws Exception {
        if (chunk.isEmpty()) {
            log.debug("빈 청크 - 처리할 아이템 없음");
            return;
        }

        log.info("배치 Writer 시작 - 청크 크기: {}개", chunk.size());

        ChunkStatistics chunkStats = new ChunkStatistics();

        // 청크 내 각 아이템 처리
        for (ForecastBatchItem item : chunk) {
            try {
                processItemAndUpdateStatistics(item, chunkStats);
            } catch (Exception e) {
                log.error("아이템 처리 중 Writer에서 예외 발생 - sellerId: {}", item.sellerId(), e);
                chunkStats.incrementFailure(0);
            }
        }

        // 전체 통계 업데이트
        updateGlobalStatistics(chunkStats);

        // 청크 처리 결과 로깅
        logChunkResults(chunkStats);
    }

    /**
     * 개별 아이템 처리 및 통계 업데이트
     */
    private void processItemAndUpdateStatistics(ForecastBatchItem item, ChunkStatistics chunkStats) {
        if (item.processingResult() == null) {
            log.warn("처리 결과 없음 - sellerId: {}, vendorName: {}", item.sellerId(), item.vendorName());
            chunkStats.incrementFailure(0);
            return;
        }

        ForecastBatchItem.ProcessingResult result = item.processingResult();

        if (result.success()) {
            handleSuccessfulProcessing(item, result, chunkStats);
        } else if (result.isSkipped()) {
            handleSkippedProcessing(item, result, chunkStats);
        } else {
            handleFailedProcessing(item, result, chunkStats);
        }
    }

    /**
     * 성공 처리 로깅 및 통계 업데이트
     */
    private void handleSuccessfulProcessing(ForecastBatchItem item,
                                            ForecastBatchItem.ProcessingResult result,
                                            ChunkStatistics chunkStats) {

        log.info("수요예측 성공 - sellerId: {}, vendorName: {}, 처리상품: {}개, 신뢰도: {:.3f}, 소요시간: {}ms",
                item.sellerId(), item.vendorName(), result.processedProductCount(),
                result.averageConfidenceScore(), result.processingTimeMs());

        // 통계 업데이트
        chunkStats.incrementSuccess(
                result.processingTimeMs(),
                result.processedProductCount(),
                result.shortageProductCount(),
                result.averageConfidenceScore()
        );

        // 품질 분석
        analyzeQuality(item, result, chunkStats);

        // 재고 부족 경고
        if (result.shortageProductCount() > 0) {
            log.warn("재고 부족 상품 발견 - sellerId: {}, 부족상품: {}개",
                    item.sellerId(), result.shortageProductCount());
        }
    }

    /**
     * 스킵 처리 로깅
     */
    private void handleSkippedProcessing(ForecastBatchItem item,
                                         ForecastBatchItem.ProcessingResult result,
                                         ChunkStatistics chunkStats) {

        log.debug("⏭️ 처리 스킵 - sellerId: {}, vendorName: {}, 사유: {}",
                item.sellerId(), item.vendorName(), result.errorMessage());

        chunkStats.incrementSkipped();
    }

    /**
     * 실패 처리 로깅
     */
    private void handleFailedProcessing(ForecastBatchItem item,
                                        ForecastBatchItem.ProcessingResult result,
                                        ChunkStatistics chunkStats) {

        log.error("수요예측 실패 - sellerId: {}, vendorName: {}, 오류: {}, 소요시간: {}ms",
                item.sellerId(), item.vendorName(), result.errorMessage(), result.processingTimeMs());

        chunkStats.incrementFailure(result.processingTimeMs());
    }

    /**
     * 예측 품질 분석
     */
    private void analyzeQuality(ForecastBatchItem item,
                                ForecastBatchItem.ProcessingResult result,
                                ChunkStatistics chunkStats) {

        Double confidence = result.averageConfidenceScore();
        if (confidence != null) {
            if (confidence >= 0.8) {
                chunkStats.incrementHighQuality();
                log.debug("고품질 예측 - sellerId: {}, 신뢰도: {:.3f}", item.sellerId(), confidence);
            } else if (confidence < 0.5) {
                chunkStats.incrementLowQuality();
                log.warn("저품질 예측 - sellerId: {}, 신뢰도: {:.3f}", item.sellerId(), confidence);
            }
        }

        // 높은 재고 부족 경고
        if (result.shortageProductCount() >= 5) {
            chunkStats.incrementHighShortage();
            log.warn("높은 재고 부족 - sellerId: {}, 부족상품: {}개",
                    item.sellerId(), result.shortageProductCount());
        }
    }

    /**
     * 전체 통계 업데이트 (원자적 연산)
     */
    private void updateGlobalStatistics(ChunkStatistics chunkStats) {
        totalProcessedCount.addAndGet(chunkStats.getTotalCount());
        successCount.addAndGet(chunkStats.getSuccessCount());
        failureCount.addAndGet(chunkStats.getFailureCount());
        skippedCount.addAndGet(chunkStats.getSkippedCount());
        totalProcessingTime.addAndGet(chunkStats.getTotalProcessingTime());
        totalForecastedProducts.addAndGet(chunkStats.getTotalForecastedProducts());
        totalShortageProducts.addAndGet(chunkStats.getTotalShortageProducts());

        // 신뢰도 점수 누적 (동기화 필요)
        synchronized (this) {
            confidenceScoreSum.updateAndGet(current -> current + chunkStats.getConfidenceScoreSum());
            confidenceScoreCount.addAndGet(chunkStats.getConfidenceScoreCount());
        }

        // 품질 분석 통계
        highQualityPredictions.addAndGet(chunkStats.getHighQualityCount());
        lowQualityPredictions.addAndGet(chunkStats.getLowQualityCount());
        highShortageVendors.addAndGet(chunkStats.getHighShortageCount());
    }

    /**
     * 청크 처리 결과 로깅
     */
    private void logChunkResults(ChunkStatistics chunkStats) {
        log.info("청크 처리 완료 - 성공: {}개, 실패: {}개, 스킵: {}개, 예측상품: {}개, 처리시간: {}ms",
                chunkStats.getSuccessCount(),
                chunkStats.getFailureCount(),
                chunkStats.getSkippedCount(),
                chunkStats.getTotalForecastedProducts(),
                chunkStats.getTotalProcessingTime());

        // 품질 통계 로깅
        if (chunkStats.getSuccessCount() > 0) {
            log.info("품질 분석 - 고품질: {}개, 저품질: {}개, 높은재고부족: {}개",
                    chunkStats.getHighQualityCount(),
                    chunkStats.getLowQualityCount(),
                    chunkStats.getHighShortageCount());
        }
    }


    /**
     * 청크별 통계 임시 저장 클래스
     */
    private static class ChunkStatistics {
        private int successCount = 0;
        private int failureCount = 0;
        private int skippedCount = 0;
        private long totalProcessingTime = 0;
        private int totalForecastedProducts = 0;
        private int totalShortageProducts = 0;
        private double confidenceScoreSum = 0.0;
        private int confidenceScoreCount = 0;
        private int highQualityCount = 0;
        private int lowQualityCount = 0;
        private int highShortageCount = 0;

        public void incrementSuccess(long processingTime, int forecastedProducts,
                                     int shortageProducts, Double confidenceScore) {
            successCount++;
            totalProcessingTime += processingTime;
            totalForecastedProducts += forecastedProducts;
            totalShortageProducts += shortageProducts;

            if (confidenceScore != null) {
                confidenceScoreSum += confidenceScore;
                confidenceScoreCount++;
            }
        }

        public void incrementFailure(long processingTime) {
            failureCount++;
            totalProcessingTime += processingTime;
        }

        public void incrementSkipped() {
            skippedCount++;
        }

        public void incrementHighQuality() { highQualityCount++; }
        public void incrementLowQuality() { lowQualityCount++; }
        public void incrementHighShortage() { highShortageCount++; }

        // Getter 메서드들
        public int getTotalCount() { return successCount + failureCount + skippedCount; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public int getSkippedCount() { return skippedCount; }
        public long getTotalProcessingTime() { return totalProcessingTime; }
        public int getTotalForecastedProducts() { return totalForecastedProducts; }
        public int getTotalShortageProducts() { return totalShortageProducts; }
        public double getConfidenceScoreSum() { return confidenceScoreSum; }
        public int getConfidenceScoreCount() { return confidenceScoreCount; }
        public int getHighQualityCount() { return highQualityCount; }
        public int getLowQualityCount() { return lowQualityCount; }
        public int getHighShortageCount() { return highShortageCount; }
    }

}