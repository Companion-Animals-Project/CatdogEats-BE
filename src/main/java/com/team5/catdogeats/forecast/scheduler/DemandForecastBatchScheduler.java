package com.team5.catdogeats.forecast.scheduler;

import com.team5.catdogeats.forecast.service.DailySalesAggregationService;
import com.team5.catdogeats.forecast.service.DemandForecastService;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 수요예측 배치 스케줄러
 * 매일 새벽 3시에 실행되어 일별 판매 집계 및 수요예측을 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "batch.demand-forecast.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DemandForecastBatchScheduler {

    private final DailySalesAggregationService aggregationService;
    private final DemandForecastService forecastService;
    private final SellersRepository sellersRepository;

    /**
     * 매일 새벽 3시 수요예측 배치 실행
     * 1. 어제 판매 데이터 집계
     * 2. 모든 판매자의 수요예측 실행
     * 3. 30일 이전 예측 데이터 정리
     */
    @Scheduled(cron = "0 0 3 * * ?") // 매일 새벽 3시
    public void runDemandForecastBatch() {
        log.info("=== 수요예측 배치 작업 시작 ===");
        long startTime = System.currentTimeMillis();

        try {
            // 1. 어제 판매 데이터 집계
            LocalDate yesterday = LocalDate.now().minusDays(1);
            int aggregatedRecords = aggregationService.aggregateDailySales(yesterday);
            log.info("일별 판매 집계 완료 - 날짜: {}, 처리 건수: {}", yesterday, aggregatedRecords);

            // 2. 모든 활성 판매자의 수요예측 실행
            List<String> activeSellerIds = sellersRepository.findAll()
                    .stream()
                    .filter(seller -> !seller.isDeleted())
                    .map(Sellers::getUserId)
                    .toList();
            log.info("수요예측 대상 판매자 수: {}", activeSellerIds.size());

            AtomicInteger totalForecastedProducts = new AtomicInteger(0);
            AtomicInteger successfulSellers = new AtomicInteger(0);

            for (String sellerId : activeSellerIds) {
                try {
                    int forecastedProducts = forecastService.executeForecasting(sellerId);
                    totalForecastedProducts.addAndGet(forecastedProducts);

                    if (forecastedProducts > 0) {
                        successfulSellers.incrementAndGet();
                    }

                    log.debug("판매자 수요예측 완료 - sellerId: {}, 예측 상품 수: {}", sellerId, forecastedProducts);

                    // 과부하 방지를 위한 짧은 대기
                    Thread.sleep(100);

                } catch (Exception e) {
                    log.error("판매자 수요예측 실패 - sellerId: {}", sellerId, e);
                }
            }

            // 3. 30일 이전 예측 데이터 정리
            LocalDate cleanupCutoff = LocalDate.now().minusDays(30);
            int cleanedRecords = forecastService.cleanupOldForecasts(cleanupCutoff);
            log.info("오래된 예측 데이터 정리 완료 - 삭제 건수: {}", cleanedRecords);

            // 4. 배치 실행 결과 요약
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            log.info("=== 수요예측 배치 작업 완료 ===");
            log.info("실행 시간: {}ms", executionTime);
            log.info("일별 집계: {} 건", aggregatedRecords);
            log.info("예측 성공 판매자: {}/{}", successfulSellers.get(), activeSellerIds.size());
            log.info("총 예측 상품 수: {}", totalForecastedProducts.get());
            log.info("정리된 레코드: {} 건", cleanedRecords);

        } catch (Exception e) {
            log.error("수요예측 배치 작업 중 예상치 못한 오류 발생", e);
        }
    }

    /**
     * 수동 실행용 메서드 - 특정 날짜 집계
     */
    public BatchExecutionResult runManualAggregation(LocalDate targetDate) {
        try {
            log.info("수동 일별 집계 시작 - targetDate: {}", targetDate);

            long startTime = System.currentTimeMillis();
            int processedRecords = aggregationService.aggregateDailySales(targetDate);
            long endTime = System.currentTimeMillis();

            String summary = String.format("날짜: %s, 처리 건수: %d, 실행시간: %dms",
                    targetDate, processedRecords, endTime - startTime);

            log.info("수동 일별 집계 완료 - {}", summary);

            return new BatchExecutionResult(true, summary, processedRecords);

        } catch (Exception e) {
            log.error("수동 일별 집계 실패 - targetDate: {}", targetDate, e);
            return new BatchExecutionResult(false, "집계 실패: " + e.getMessage(), 0);
        }
    }

    /**
     * 수동 실행용 메서드 - 특정 판매자 예측
     */
    public BatchExecutionResult runManualForecast(String sellerId) {
        try {
            log.info("수동 수요예측 시작 - sellerId: {}", sellerId);

            long startTime = System.currentTimeMillis();
            int forecastedProducts = forecastService.executeForecasting(sellerId);
            long endTime = System.currentTimeMillis();

            String summary = String.format("판매자: %s, 예측 상품: %d개, 실행시간: %dms",
                    sellerId, forecastedProducts, endTime - startTime);

            log.info("수동 수요예측 완료 - {}", summary);

            return new BatchExecutionResult(true, summary, forecastedProducts);

        } catch (Exception e) {
            log.error("수동 수요예측 실패 - sellerId: {}", sellerId, e);
            return new BatchExecutionResult(false, "예측 실패: " + e.getMessage(), 0);
        }
    }

    /**
     * 배치 실행 결과 DTO
     */
    public record BatchExecutionResult(
            boolean success,
            String message,
            int processedCount
    ) {
    }
}