package com.team5.catdogeats.forecast.service.impl;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.forecast.domain.DemandForecasts;
import com.team5.catdogeats.forecast.domain.dto.DailySalesDataDTO;
import com.team5.catdogeats.forecast.domain.dto.DemandForecastResultDTO;
import com.team5.catdogeats.forecast.mapper.DailySalesAggregationMapper;
import com.team5.catdogeats.forecast.mapper.DemandForecastMapper;
import com.team5.catdogeats.forecast.service.DemandForecastService;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemandForecastServiceImpl implements DemandForecastService {

    private final DemandForecastMapper demandForecastMapper;
    private final DailySalesAggregationMapper dailySalesMapper;


    // 예측 설정 상수
    private static final int HISTORICAL_DAYS = 30;      // 30일간 데이터 필요
    private static final int MIN_SALES_DAYS = 15;       // 최소 15일간 판매 기록
    private static final int PREDICTION_DAYS = 7;       // 7일간 예측
    private static final int MOVING_AVERAGE_WINDOW = 7; // 7일 이동평균

    @Override
    @MybatisTransactional
    public int executeForecasting(String sellerId) {
        log.info("수요예측 실행 시작 - sellerId: {}", sellerId);

        try {

            Sellers seller = dailySalesMapper.findSellerById(sellerId);
            if (seller == null) {
                throw new IllegalArgumentException("판매자를 찾을 수 없습니다: " + sellerId);
            }

            // 2. 충분한 판매 데이터가 있는 상품 목록 조회 (MyBatis)
            LocalDate endDate = LocalDate.now().minusDays(1); // 어제까지
            LocalDate startDate = endDate.minusDays(HISTORICAL_DAYS);

            List<String> eligibleProducts = dailySalesMapper.findProductsWithSufficientData(
                    sellerId, startDate, MIN_SALES_DAYS);

            if (eligibleProducts.isEmpty()) {
                log.info("예측 가능한 상품이 없습니다 - sellerId: {}", sellerId);
                return 0;
            }

            log.info("예측 대상 상품 수: {} - sellerId: {}", eligibleProducts.size(), sellerId);

            // 3. 기존 오늘자 예측 데이터 삭제 (재실행 대비) (MyBatis)
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

            // 2. 판매 데이터 조회 (MyBatis)
            List<DailySalesDataDTO> salesData = dailySalesMapper.findSalesDataForForecast(
                    seller.getUserId(), productId, startDate, endDate);

            if (salesData.size() < MIN_SALES_DAYS) {
                log.debug("판매 데이터 부족 - productId: {}, days: {}", productId, salesData.size());
                return false;
            }

            // 3. 7일 이동평균법으로 예측
            double predictedAvgDaily = calculateMovingAverage(salesData);
            int predictedQuantity = (int) Math.ceil(predictedAvgDaily * PREDICTION_DAYS);

            // 4. 신뢰도 점수 계산 (판매 일수 기반)
            double confidenceScore = Math.min(salesData.size() / (double) HISTORICAL_DAYS, 1.0);

            // 5. 예측 결과 저장
            saveForecastResult(seller, product, predictedQuantity, confidenceScore, salesData.size());

            log.debug("예측 완료 - productId: {}, 예측량: {}, 신뢰도: {:.2f}",
                    productId, predictedQuantity, confidenceScore);
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
                    .predictionPeriodDays(PREDICTION_DAYS)
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
     * 7일 이동평균 계산
     */
    private double calculateMovingAverage(List<DailySalesDataDTO> salesData) {
        if (salesData.size() <= MOVING_AVERAGE_WINDOW) {
            // 데이터가 적으면 전체 평균
            return salesData.stream()
                    .mapToInt(DailySalesDataDTO::dailyQuantity)
                    .average()
                    .orElse(0.0);
        }

        // 최근 7일 이동평균
        int recentDataCount = Math.min(MOVING_AVERAGE_WINDOW, salesData.size());
        return salesData.subList(salesData.size() - recentDataCount, salesData.size())
                .stream()
                .mapToInt(DailySalesDataDTO::dailyQuantity)
                .average()
                .orElse(0.0);
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