package com.team5.catdogeats.forecast.service.impl;

import com.team5.catdogeats.batch.config.ForecastBatchProperties;
import com.team5.catdogeats.forecast.domain.DemandForecasts;
import com.team5.catdogeats.forecast.domain.dto.DailySalesDataDTO;
import com.team5.catdogeats.forecast.mapper.DailySalesAggregationMapper;
import com.team5.catdogeats.forecast.mapper.DemandForecastMapper;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("수요예측 서비스 테스트")
class DemandForecastServiceImplTest {

    @InjectMocks
    private DemandForecastServiceImpl demandForecastService;

    @Mock
    private DemandForecastMapper demandForecastMapper;

    @Mock
    private DailySalesAggregationMapper dailySalesMapper;

    @Mock
    private ForecastBatchProperties batchProperties;

    @Mock
    private Sellers mockSeller;

    @Mock
    private Products mockProduct;

    private String sellerId;
    private String productId;
    private LocalDate endDate;
    private LocalDate startDate;
    private List<DailySalesDataDTO> salesData;

    @BeforeEach
    void setUp() {
        sellerId = "seller123";
        productId = "product123";
        endDate = LocalDate.now().minusDays(1);
        startDate = endDate.minusDays(30);

        // ForecastBatchProperties 기본값 설정
        given(batchProperties.getHistoricalDataDays()).willReturn(30);
        given(batchProperties.getMinSalesDataDays()).willReturn(7);
        given(batchProperties.getPredictionPeriodDays()).willReturn(7);
        given(batchProperties.getMovingAverageWindow()).willReturn(7);

        // 테스트용 판매 데이터 생성 (7일간)
        salesData = Arrays.asList(
                new DailySalesDataDTO(sellerId, productId, endDate.minusDays(6), 10, Long.valueOf(50000), 2),
                new DailySalesDataDTO(sellerId, productId, endDate.minusDays(5), 12, Long.valueOf(60000), 3),
                new DailySalesDataDTO(sellerId, productId, endDate.minusDays(4), 8, Long.valueOf(40000), 2),
                new DailySalesDataDTO(sellerId, productId, endDate.minusDays(3), 15, Long.valueOf(75000), 4),
                new DailySalesDataDTO(sellerId, productId, endDate.minusDays(2), 9, Long.valueOf(45000), 2),
                new DailySalesDataDTO(sellerId, productId, endDate.minusDays(1), 11, Long.valueOf(55000), 3),
                new DailySalesDataDTO(sellerId, productId, endDate, 13, Long.valueOf(65000), 3)
        );
    }

    @Test
    @DisplayName("정상적인 수요예측 실행 - 단일 상품")
    void executeForecasting_Success_SingleProduct() {
        // Given
        given(dailySalesMapper.findSellerById(sellerId)).willReturn(mockSeller);
        given(mockSeller.getUserId()).willReturn(sellerId);

        given(dailySalesMapper.findProductsWithSufficientData(eq(sellerId), any(LocalDate.class), eq(7)))
                .willReturn(Arrays.asList(productId));

        given(dailySalesMapper.findProductById(productId)).willReturn(mockProduct);

        given(dailySalesMapper.findSalesDataForForecast(eq(sellerId), eq(productId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(salesData);

        // When
        int result = demandForecastService.executeForecasting(sellerId);

        // Then
        assertThat(result).isEqualTo(1);

        // 기존 예측 데이터 삭제 확인
        verify(demandForecastMapper).deleteForecastsBySellerAndDate(eq(sellerId), any(LocalDate.class));

        // 예측 결과 저장 확인
        verify(demandForecastMapper).insertForecast(any(DemandForecasts.class));
    }

    @Test
    @DisplayName("정상적인 수요예측 실행 - 다중 상품")
    void executeForecasting_Success_MultipleProducts() {
        // Given
        List<String> productIds = Arrays.asList("product1", "product2", "product3");

        given(dailySalesMapper.findSellerById(sellerId)).willReturn(mockSeller);
        given(mockSeller.getUserId()).willReturn(sellerId);

        given(dailySalesMapper.findProductsWithSufficientData(eq(sellerId), any(LocalDate.class), eq(7)))
                .willReturn(productIds);

        given(dailySalesMapper.findProductById(anyString())).willReturn(mockProduct);

        given(dailySalesMapper.findSalesDataForForecast(eq(sellerId), anyString(), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(salesData);

        // When
        int result = demandForecastService.executeForecasting(sellerId);

        // Then
        assertThat(result).isEqualTo(3);

        // 예측 결과가 3번 저장되었는지 확인
        verify(demandForecastMapper, times(3)).insertForecast(any(DemandForecasts.class));
    }

    @Test
    @DisplayName("판매자를 찾을 수 없는 경우")
    void executeForecasting_SellerNotFound() {
        // Given
        given(dailySalesMapper.findSellerById(sellerId)).willReturn(null);

        // When & Then
        // 실제 구현체는 IllegalArgumentException을 RuntimeException으로 래핑함
        assertThatThrownBy(() -> demandForecastService.executeForecasting(sellerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("수요예측 실행 중 오류가 발생했습니다");
    }

    @Test
    @DisplayName("예측 가능한 상품이 없는 경우")
    void executeForecasting_NoEligibleProducts() {
        // Given
        given(dailySalesMapper.findSellerById(sellerId)).willReturn(mockSeller);
        given(dailySalesMapper.findProductsWithSufficientData(eq(sellerId), any(LocalDate.class), eq(7)))
                .willReturn(Collections.emptyList());

        // When
        int result = demandForecastService.executeForecasting(sellerId);

        // Then
        assertThat(result).isEqualTo(0);

        // 예측 결과 저장이 호출되지 않았는지 확인
        verify(demandForecastMapper, never()).insertForecast(any(DemandForecasts.class));
    }

    @Test
    @DisplayName("판매 데이터 부족으로 인한 예측 실패")
    void executeForecasting_InsufficientSalesData() {
        // Given
        List<DailySalesDataDTO> insufficientData = Arrays.asList(
                new DailySalesDataDTO(sellerId, productId, endDate.minusDays(2), 10, Long.valueOf(50000), 2),
                new DailySalesDataDTO(sellerId, productId, endDate.minusDays(1), 12, Long.valueOf(60000), 3)
        ); // 2일치 데이터만 (최소 7일 필요)

        given(dailySalesMapper.findSellerById(sellerId)).willReturn(mockSeller);
        given(mockSeller.getUserId()).willReturn(sellerId);

        given(dailySalesMapper.findProductsWithSufficientData(eq(sellerId), any(LocalDate.class), eq(7)))
                .willReturn(Arrays.asList(productId));

        given(dailySalesMapper.findProductById(productId)).willReturn(mockProduct);

        given(dailySalesMapper.findSalesDataForForecast(eq(sellerId), eq(productId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(insufficientData);

        // When
        int result = demandForecastService.executeForecasting(sellerId);

        // Then
        assertThat(result).isEqualTo(0);

        // 예측 결과 저장이 호출되지 않았는지 확인
        verify(demandForecastMapper, never()).insertForecast(any(DemandForecasts.class));
    }

    @Test
    @DisplayName("이동평균 계산 테스트 - 데이터가 윈도우 크기보다 적은 경우")
    void executeForecasting_MovingAverage_SmallDataset() {
        // Given
        List<DailySalesDataDTO> smallData = Arrays.asList(
                new DailySalesDataDTO(sellerId, productId, endDate.minusDays(2), 10, Long.valueOf(50000), 2),
                new DailySalesDataDTO(sellerId, productId, endDate.minusDays(1), 20, Long.valueOf(100000), 4),
                new DailySalesDataDTO(sellerId, productId, endDate, 30, Long.valueOf(150000), 6)
        ); // 3일치 데이터 (평균: 20)

        // 윈도우 크기를 7일로 설정했지만, 데이터는 3일만 있음
        given(batchProperties.getMovingAverageWindow()).willReturn(7);
        // 최소 판매 데이터를 3일로 설정하여 이 테스트가 실행되도록 함
        given(batchProperties.getMinSalesDataDays()).willReturn(3);

        given(dailySalesMapper.findSellerById(sellerId)).willReturn(mockSeller);
        given(mockSeller.getUserId()).willReturn(sellerId);

        given(dailySalesMapper.findProductsWithSufficientData(eq(sellerId), any(LocalDate.class), eq(3)))
                .willReturn(Arrays.asList(productId));

        given(dailySalesMapper.findProductById(productId)).willReturn(mockProduct);

        given(dailySalesMapper.findSalesDataForForecast(eq(sellerId), eq(productId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(smallData);

        // When
        int result = demandForecastService.executeForecasting(sellerId);

        // Then
        assertThat(result).isEqualTo(1);

        // 전체 평균이 사용되어야 함 (20 * 7일 = 140개 예측)
        verify(demandForecastMapper).insertForecast(argThat(forecast ->
                forecast.getPredictedQuantity() == 140 // ceil(20 * 7)
        ));
    }

    @Test
    @DisplayName("신뢰도 점수 계산 테스트")
    void executeForecasting_ConfidenceScore_Calculation() {
        // Given
        // 30일 설정에서 7일 데이터가 있으면 신뢰도는 7/30 = 0.233...
        given(dailySalesMapper.findSellerById(sellerId)).willReturn(mockSeller);
        given(mockSeller.getUserId()).willReturn(sellerId);

        given(dailySalesMapper.findProductsWithSufficientData(eq(sellerId), any(LocalDate.class), eq(7)))
                .willReturn(Arrays.asList(productId));

        given(dailySalesMapper.findProductById(productId)).willReturn(mockProduct);

        given(dailySalesMapper.findSalesDataForForecast(eq(sellerId), eq(productId), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(salesData); // 7일 데이터

        // When
        int result = demandForecastService.executeForecasting(sellerId);

        // Then
        assertThat(result).isEqualTo(1);

        // 신뢰도 점수가 올바르게 계산되었는지 확인 (7/30 = 0.233...)
        verify(demandForecastMapper).insertForecast(argThat(forecast ->
                Math.abs(forecast.getConfidenceScore() - (7.0/30.0)) < 0.001
        ));
    }

    @Test
    @DisplayName("오래된 예측 데이터 정리 테스트")
    void cleanupOldForecasts_Success() {
        // Given
        LocalDate cutoffDate = LocalDate.now().minusDays(30);
        given(demandForecastMapper.deleteOldForecasts(cutoffDate)).willReturn(5);

        // When
        int deletedCount = demandForecastService.cleanupOldForecasts(cutoffDate);

        // Then
        assertThat(deletedCount).isEqualTo(5);
        verify(demandForecastMapper).deleteOldForecasts(cutoffDate);
    }

    @Test
    @DisplayName("예측 실행 중 예외 발생 테스트")
    void executeForecasting_Exception_During_Execution() {
        // Given
        given(dailySalesMapper.findSellerById(sellerId)).willReturn(mockSeller);
        given(mockSeller.getUserId()).willReturn(sellerId);

        given(dailySalesMapper.findProductsWithSufficientData(eq(sellerId), any(LocalDate.class), eq(7)))
                .willThrow(new RuntimeException("데이터베이스 오류"));

        // When & Then
        assertThatThrownBy(() -> demandForecastService.executeForecasting(sellerId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("수요예측 실행 중 오류가 발생했습니다");
    }

    @Test
    @DisplayName("혼합 상황 - 일부 상품 성공, 일부 실패")
    void executeForecasting_PartialSuccess() {
        // Given
        List<String> productIds = Arrays.asList("product1", "product2", "product3");

        given(dailySalesMapper.findSellerById(sellerId)).willReturn(mockSeller);
        given(mockSeller.getUserId()).willReturn(sellerId);

        given(dailySalesMapper.findProductsWithSufficientData(eq(sellerId), any(LocalDate.class), eq(7)))
                .willReturn(productIds);

        // product1과 product3는 성공, product2는 상품을 찾을 수 없음
        given(dailySalesMapper.findProductById("product1")).willReturn(mockProduct);
        given(dailySalesMapper.findProductById("product2")).willReturn(null);
        given(dailySalesMapper.findProductById("product3")).willReturn(mockProduct);

        given(dailySalesMapper.findSalesDataForForecast(eq(sellerId), eq("product1"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(salesData);
        given(dailySalesMapper.findSalesDataForForecast(eq(sellerId), eq("product3"), any(LocalDate.class), any(LocalDate.class)))
                .willReturn(salesData);

        // When
        int result = demandForecastService.executeForecasting(sellerId);

        // Then
        assertThat(result).isEqualTo(2); // 3개 중 2개만 성공

        // 성공한 상품에 대해서만 예측 결과 저장
        verify(demandForecastMapper, times(2)).insertForecast(any(DemandForecasts.class));
    }
}