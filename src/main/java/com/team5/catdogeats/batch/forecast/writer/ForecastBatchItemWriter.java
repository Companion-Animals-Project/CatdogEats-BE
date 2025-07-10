package com.team5.catdogeats.batch.forecast.writer;

import com.team5.catdogeats.batch.forecast.dto.ForecastBatchItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 수요예측 배치 ItemWriter
 * 처리 결과를 로깅하고 통계를 수집
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

    /**
     * 청크 단위로 처리 결과 기록
     */
    @Override
    public void write(Chunk<? extends ForecastBatchItem> chunk) throws Exception {
        if (chunk.isEmpty()) {
            log.debug("빈 청크 - 처리할 아이템 없음");
            return;
        }

        log.info("배치 Writer 시작 - 청크 크기: {}개", chunk.size());

        int chunkSuccessCount = 0;
        int chunkFailureCount = 0;
        int chunkSkippedCount = 0;
        long chunkProcessingTime = 0;
        int chunkForecastedProducts = 0;
        int chunkShortageProducts = 0;

        // 청크 내 각 아이템 처리
        for (ForecastBatchItem item : chunk) {
            try {
                processItem(item);

                // 청크 통계 업데이트
                if (item.getProcessingResult() != null) {
                    chunkProcessingTime += item.getProcessingResult().getProcessingTimeMs();

                    if (item.getProcessingResult().isSuccess()) {
                        if (item.getProcessingResult().getProcessedProductCount() > 0) {
                            chunkSuccessCount++;
                            chunkForecastedProducts += item.getProcessingResult().getProcessedProductCount();
                            chunkShortageProducts += item.getProcessingResult().getShortageProductCount();
                        } else {
                            chunkSkippedCount++;
                        }
                    } else {
                        chunkFailureCount++;
                    }
                }

            } catch (Exception e) {
                log.error("아이템 처리 중 오류 - sellerId: {}", item.getSellerId(), e);
                chunkFailureCount++;
            }
        }

        // 전체 통계 업데이트
        totalProcessedCount.addAndGet(chunk.size());
        successCount.addAndGet(chunkSuccessCount);
        failureCount.addAndGet(chunkFailureCount);
        skippedCount.addAndGet(chunkSkippedCount);
        totalProcessingTime.addAndGet(chunkProcessingTime);
        totalForecastedProducts.addAndGet(chunkForecastedProducts);
        totalShortageProducts.addAndGet(chunkShortageProducts);

        // 청크 처리 결과 로깅
        log.info("배치 Writer 완료 - 청크 결과: 성공 {}개, 실패 {}개, 스킵 {}개, 예측상품 {}개, 부족상품 {}개",
                chunkSuccessCount, chunkFailureCount, chunkSkippedCount,
                chunkForecastedProducts, chunkShortageProducts);

        // 현재까지 전체 진행 상황 로깅
        logProgressSummary();
    }

    /**
     * 개별 아이템 처리
     */
    private void processItem(ForecastBatchItem item) {
        if (item.getProcessingResult() == null) {
            log.warn("처리 결과가 없는 아이템 - sellerId: {}", item.getSellerId());
            return;
        }

        ForecastBatchItem.ProcessingResult result = item.getProcessingResult();

        if (result.isSuccess()) {
            if (result.getProcessedProductCount() > 0) {
                // 성공 케이스
                log.info("✅ 예측 성공 - 판매자: {} ({}), 예측상품: {}개, 부족상품: {}개, 신뢰도: {:.2f}, 소요시간: {}ms",
                        item.getVendorName(), item.getSellerId(),
                        result.getProcessedProductCount(),
                        result.getShortageProductCount(),
                        result.getAverageConfidenceScore() != null ? result.getAverageConfidenceScore() : 0.0,
                        result.getProcessingTimeMs());

                // 재고 부족 경고
                if (result.getShortageProductCount() > 0) {
                    log.warn("⚠️ 재고 부족 상품 발견 - 판매자: {} ({}), 부족상품: {}개",
                            item.getVendorName(), item.getSellerId(), result.getShortageProductCount());
                }

                // 낮은 신뢰도 경고
                if (result.getAverageConfidenceScore() != null && result.getAverageConfidenceScore() < 0.3) {
                    log.warn("⚠️ 낮은 신뢰도 예측 - 판매자: {} ({}), 신뢰도: {:.2f}",
                            item.getVendorName(), item.getSellerId(), result.getAverageConfidenceScore());
                }

            } else {
                // 스킵 케이스
                log.debug("⏭️ 예측 스킵 - 판매자: {} ({}), 사유: {}",
                        item.getVendorName(), item.getSellerId(),
                        result.getErrorMessage() != null ? result.getErrorMessage() : "알 수 없음");
            }
        } else {
            // 실패 케이스
            log.error("❌ 예측 실패 - 판매자: {} ({}), 오류: {}, 소요시간: {}ms",
                    item.getVendorName(), item.getSellerId(),
                    result.getErrorMessage(), result.getProcessingTimeMs());
        }
    }

    /**
     * 진행 상황 요약 로깅
     */
    private void logProgressSummary() {
        int processed = totalProcessedCount.get();
        if (processed > 0 && processed % 50 == 0) { // 50개마다 진행상황 로깅
            double successRate = (double) successCount.get() / processed * 100;
            double avgProcessingTime = totalProcessingTime.get() / (double) processed;

            log.info("📊 진행 상황 - 처리: {}개, 성공률: {:.1f}%, 평균 처리시간: {:.1f}ms, 총 예측상품: {}개, 총 부족상품: {}개",
                    processed, successRate, avgProcessingTime,
                    totalForecastedProducts.get(), totalShortageProducts.get());
        }
    }

    /**
     * 최종 통계 조회
     */
    public WriterStatistics getFinalStatistics() {
        int processed = totalProcessedCount.get();
        double successRate = processed > 0 ? (double) successCount.get() / processed * 100 : 0.0;
        double avgProcessingTime = processed > 0 ? totalProcessingTime.get() / (double) processed : 0.0;

        return WriterStatistics.builder()
                .totalProcessedCount(processed)
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .skippedCount(skippedCount.get())
                .successRate(successRate)
                .totalProcessingTimeMs(totalProcessingTime.get())
                .averageProcessingTimeMs(avgProcessingTime)
                .totalForecastedProducts(totalForecastedProducts.get())
                .totalShortageProducts(totalShortageProducts.get())
                .build();
    }

    /**
     * 통계 초기화 (새로운 배치 시작시)
     */
    public void resetStatistics() {
        totalProcessedCount.set(0);
        successCount.set(0);
        failureCount.set(0);
        skippedCount.set(0);
        totalProcessingTime.set(0);
        totalForecastedProducts.set(0);
        totalShortageProducts.set(0);

        log.info("Writer 통계 초기화 완료");
    }

    /**
     * Writer 통계 정보
     */
    @lombok.Builder
    @lombok.Getter
    public static class WriterStatistics {
        private final int totalProcessedCount;
        private final int successCount;
        private final int failureCount;
        private final int skippedCount;
        private final double successRate;
        private final long totalProcessingTimeMs;
        private final double averageProcessingTimeMs;
        private final int totalForecastedProducts;
        private final int totalShortageProducts;

        @Override
        public String toString() {
            return String.format(
                    "Writer 통계 - 처리: %d개, 성공: %d개(%.1f%%), 실패: %d개, 스킵: %d개, " +
                            "총 처리시간: %dms, 평균 처리시간: %.1fms, 예측상품: %d개, 부족상품: %d개",
                    totalProcessedCount, successCount, successRate, failureCount, skippedCount,
                    totalProcessingTimeMs, averageProcessingTimeMs, totalForecastedProducts, totalShortageProducts
            );
        }
    }
}