package com.team5.catdogeats.forecast.service.impl;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.batch.forecast.config.ForecastBatchProperties;
import com.team5.catdogeats.forecast.domain.DemandForecasts;
import com.team5.catdogeats.forecast.domain.dto.DailySalesDataDTO;
import com.team5.catdogeats.forecast.domain.dto.DemandForecastResultDTO;
import com.team5.catdogeats.forecast.mapper.DailySalesAggregationMapper;
import com.team5.catdogeats.forecast.mapper.DemandForecastMapper;
import com.team5.catdogeats.forecast.service.DemandForecastService;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 수요예측 서비스 구현체
 * 7일 이동평균법을 사용한 상품별 수요예측
 * 설정값은 ForecastBatchProperties를 통해 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandForecastServiceImpl implements DemandForecastService {

    private final DemandForecastMapper demandForecastMapper;
    private final DailySalesAggregationMapper dailySalesMapper;
    private final ForecastBatchProperties batchProperties;

    @Override
    @MybatisTransactional
    public int executeForecasting(String sellerId) {
        log.info("수요예측 실행 시작 - sellerId: {}", sellerId);

        try {
            // 1. 판매자 존재 확인
            Sellers seller = dailySalesMapper.findSellerById(sellerId);
            if (seller == null) {
                throw new IllegalArgumentException("판매자를 찾을 수 없습니다: " + sellerId);
            }

            // 2. 충분한 판매 데이터가 있는 상품 목록 조회
            LocalDate endDate = LocalDate.now().minusDays(1); // 어제까지
            LocalDate startDate = endDate.minusDays(batchProperties.getHistoricalDataDays());

            List<String> eligibleProducts = dailySalesMapper.findProductsWithSufficientData(
                    sellerId, startDate, batchProperties.getMinSalesDataDays());

            if (eligibleProducts.isEmpty()) {
                log.info("예측 가능한 상품이 없습니다 - sellerId: {}", sellerId);
                return 0;
            }

            log.info("예측 대상 상품 수: {} - sellerId: {}, 설정 - 과거데이터: {}일, 최소판매: {}일",
                    eligibleProducts.size(), sellerId,
                    batchProperties.getHistoricalDataDays(),
                    batchProperties.getMinSalesDataDays());

            // 3. 기존 오늘자 예측 데이터 삭제
            LocalDate today = LocalDate.now();
            demandForecastMapper.deleteForecastsBySellerAndDate(sellerId, today);

            // 4. 각 상품별 예측 실행
            int successCount = 0;
            for (String productId : eligibleProducts) {
                try {
                    if (executeForecastingForProduct(seller, productId, startDate, endDate)) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("상품별 예측 실패 - sellerId: {}, productId: {}", sellerId, productId, e);
                }
            }

            log.info("수요예측 완료 - sellerId: {}, 성공: {}/{}", sellerId, successCount, eligibleProducts.size());
            return successCount;

        } catch (Exception e) {
            log.error("수요예측 실행 실패 - sellerId: {}", sellerId, e);
            throw new RuntimeException("수요예측 실행 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 개별 상품에 대한 수요예측 실행
     */
    private boolean executeForecastingForProduct(Sellers seller, String productId,
                                                 LocalDate startDate, LocalDate endDate) {
        try {
            // 1. 상품 정보 조회
            Products product = dailySalesMapper.findProductById(productId);
            if (product == null) {
                throw new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId);
            }

            // 2. 판매 데이터 조회
            List<DailySalesDataDTO> salesData = dailySalesMapper.findSalesDataForForecast(
                    seller.getUserId(), productId, startDate, endDate);

            if (salesData.size() < batchProperties.getMinSalesDataDays()) {
                log.debug("판매 데이터 부족 - productId: {}, days: {}, 필요: {}",
                        productId, salesData.size(), batchProperties.getMinSalesDataDays());
                return false;
            }

            // 3. 이동평균법으로 예측 (설정값 사용)
            double predictedAvgDaily = calculateMovingAverage(salesData);
            int predictedQuantity = (int) Math.ceil(predictedAvgDaily * batchProperties.getPredictionPeriodDays());

            // 4. 신뢰도 점수 계산 (설정값 사용)
            double confidenceScore = Math.min(salesData.size() / (double) batchProperties.getHistoricalDataDays(), 1.0);

            // 5. 예측 결과 저장
            saveForecastResult(seller, product, predictedQuantity, confidenceScore, salesData.size());

            log.debug("예측 완료 - productId: {}, 예측량: {} ({}일간), 신뢰도: {:.2f}, 이동평균윈도우: {}일",
                    productId, predictedQuantity, batchProperties.getPredictionPeriodDays(),
                    confidenceScore, batchProperties.getMovingAverageWindow());
            return true;

        } catch (Exception e) {
            log.error("상품 예측 실패 - productId: {}", productId, e);
            return false;
        }
    }

    /**
     * 예측 결과 저장 (MyBatis 전용 메서드)
     */
    private void saveForecastResult(Sellers seller, Products product, int predictedQuantity,
                                    double confidenceScore, int historicalDataDays) {
        try {
            ZonedDateTime now = ZonedDateTime.now();

            // DemandForecasts Entity 생성
            DemandForecasts forecast = DemandForecasts.builder()
                    .id(UUID.randomUUID().toString())
                    .seller(seller)
                    .product(product)
                    .forecastDate(LocalDate.now())
                    .predictionPeriodDays(batchProperties.getPredictionPeriodDays()) // 설정값 사용
                    .predictedQuantity(predictedQuantity)
                    .algorithmType(DemandForecasts.AlgorithmType.MOVING_AVERAGE_7)
                    .confidenceScore(confidenceScore)
                    .historicalDataDays(historicalDataDays)
                    .build();

            // BaseEntity 필드 직접 설정 (reflection으로)
            setTimestamps(forecast, now, now);

            // MyBatis로 저장
            demandForecastMapper.insertForecast(forecast);

        } catch (Exception e) {
            log.error("예측 결과 저장 실패 - sellerId: {}, productId: {}",
                    seller.getUserId(), product.getId(), e);
            throw new RuntimeException("예측 결과 저장 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 이동평균 계산 (설정값 기반)
     * 설정된 윈도우 크기를 사용하여 이동평균을 계산
     */
    private double calculateMovingAverage(List<DailySalesDataDTO> salesData) {
        int windowSize = batchProperties.getMovingAverageWindow();

        if (salesData.size() <= windowSize) {
            // 데이터가 윈도우 크기보다 적으면 전체 평균
            double average = salesData.stream()
                    .mapToInt(DailySalesDataDTO::dailyQuantity)
                    .average()
                    .orElse(0.0);

            log.debug("전체 평균 사용 - 데이터 수: {}, 윈도우 크기: {}, 평균: {:.2f}",
                    salesData.size(), windowSize, average);
            return average;
        }

        // 최근 N일 이동평균 (설정값 사용)
        int recentDataCount = Math.min(windowSize, salesData.size());
        double movingAverage = salesData.subList(salesData.size() - recentDataCount, salesData.size())
                .stream()
                .mapToInt(DailySalesDataDTO::dailyQuantity)
                .average()
                .orElse(0.0);

        log.debug("이동평균 계산 - 윈도우 크기: {}일, 사용 데이터: {}일, 평균: {:.2f}",
                windowSize, recentDataCount, movingAverage);
        return movingAverage;
    }

    @Override
    public List<DemandForecastResultDTO> getLatestForecastResults(String sellerId) {
        log.debug("수요예측 결과 조회 - sellerId: {}", sellerId);

        try {
            List<DemandForecastResultDTO> results = demandForecastMapper
                    .findLatestForecastsWithStockBySellerId(sellerId);

            log.debug("수요예측 결과 조회 완료 - sellerId: {}, 결과 수: {}", sellerId, results.size());
            return results;

        } catch (Exception e) {
            log.error("수요예측 결과 조회 실패 - sellerId: {}", sellerId, e);
            throw new RuntimeException("수요예측 결과 조회 중 오류가 발생했습니다", e);
        }
    }

    @Override
    public int cleanupOldForecasts(LocalDate cutoffDate) {
        log.info("오래된 수요예측 데이터 정리 시작 - cutoffDate: {}", cutoffDate);

        try {
            int deletedCount = demandForecastMapper.deleteOldForecasts(cutoffDate);
            log.info("오래된 수요예측 데이터 정리 완료 - 삭제 건수: {}", deletedCount);
            return deletedCount;

        } catch (Exception e) {
            log.error("수요예측 데이터 정리 실패 - cutoffDate: {}", cutoffDate, e);
            throw new RuntimeException("수요예측 데이터 정리 중 오류가 발생했습니다", e);
        }
    }

    /**
     * BaseEntity의 타임스탬프 필드를 설정하는 헬퍼 메서드
     */
    private void setTimestamps(DemandForecasts forecast, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        try {
            java.lang.reflect.Field createdAtField = BaseEntity.class.getDeclaredField("createdAt");
            java.lang.reflect.Field updatedAtField = BaseEntity.class.getDeclaredField("updatedAt");

            createdAtField.setAccessible(true);
            updatedAtField.setAccessible(true);

            if (createdAt != null) {
                createdAtField.set(forecast, createdAt);
            }

            if (updatedAt != null) {
                updatedAtField.set(forecast, updatedAt);
            }

        } catch (Exception e) {
            log.error("타임스탬프 설정 실패", e);
            throw new RuntimeException("타임스탬프 설정 중 오류 발생", e);
        }
    }

}