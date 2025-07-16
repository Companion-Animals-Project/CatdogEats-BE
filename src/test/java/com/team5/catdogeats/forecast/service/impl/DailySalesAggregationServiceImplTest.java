package com.team5.catdogeats.forecast.service.impl;

import com.team5.catdogeats.forecast.domain.DailySalesAggregation;
import com.team5.catdogeats.forecast.domain.dto.DailySalesDataDTO;
import com.team5.catdogeats.forecast.mapper.DailySalesAggregationMapper;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("일별 판매 집계 서비스 테스트")
class DailySalesAggregationServiceImplTest {

    @InjectMocks
    private DailySalesAggregationServiceImpl dailySalesAggregationService;

    @Mock
    private DailySalesAggregationMapper dailySalesMapper;

    @Mock
    private Sellers mockSeller;

    @Mock
    private Products mockProduct;

    private LocalDate targetDate;
    private List<DailySalesDataDTO> sampleAggregatedData;

    @BeforeEach
    void setUp() {
        targetDate = LocalDate.of(2024, 1, 15);

        // 테스트용 집계 데이터 생성
        sampleAggregatedData = Arrays.asList(
                new DailySalesDataDTO(
                        "seller1", "product1", targetDate,
                        10, Long.valueOf(50000), 3
                ),
                new DailySalesDataDTO(
                        "seller1", "product2", targetDate,
                        5, Long.valueOf(25000), 2
                ),
                new DailySalesDataDTO(
                        "seller2", "product3", targetDate,
                        15, Long.valueOf(75000), 4
                )
        );
    }

    @Test
    @DisplayName("정상적인 일별 집계 처리 - 새로운 데이터 저장")
    void aggregateDailySales_Success_NewData() {
        // Given
        given(dailySalesMapper.aggregateDailySalesByDate(targetDate))
                .willReturn(sampleAggregatedData);

        // 모든 데이터가 새로운 데이터라고 가정
        given(dailySalesMapper.existsBySellerAndProductAndDate(anyString(), anyString(), any(LocalDate.class)))
                .willReturn(false);

        given(dailySalesMapper.findSellerById("seller1")).willReturn(mockSeller);
        given(dailySalesMapper.findSellerById("seller2")).willReturn(mockSeller);
        given(dailySalesMapper.findProductById(anyString())).willReturn(mockProduct);

        // When
        int result = dailySalesAggregationService.aggregateDailySales(targetDate);

        // Then
        assertThat(result).isEqualTo(3);

        // 집계 데이터 조회가 호출되었는지 확인
        verify(dailySalesMapper).aggregateDailySalesByDate(targetDate);

        // 각 데이터에 대해 존재 여부 확인이 호출되었는지 확인
        verify(dailySalesMapper, times(3))
                .existsBySellerAndProductAndDate(anyString(), anyString(), eq(targetDate));

        // upsert가 호출되었는지 확인
        verify(dailySalesMapper, times(3)).upsertDailySales(any(DailySalesAggregation.class));
    }

    @Test
    @DisplayName("정상적인 일별 집계 처리 - 기존 데이터 업데이트")
    void aggregateDailySales_Success_ExistingData() {
        // Given
        given(dailySalesMapper.aggregateDailySalesByDate(targetDate))
                .willReturn(sampleAggregatedData);

        // 모든 데이터가 이미 존재한다고 가정
        given(dailySalesMapper.existsBySellerAndProductAndDate(anyString(), anyString(), any(LocalDate.class)))
                .willReturn(true);

        given(dailySalesMapper.findSellerById("seller1")).willReturn(mockSeller);
        given(dailySalesMapper.findSellerById("seller2")).willReturn(mockSeller);
        given(dailySalesMapper.findProductById(anyString())).willReturn(mockProduct);

        // When
        int result = dailySalesAggregationService.aggregateDailySales(targetDate);

        // Then
        assertThat(result).isEqualTo(3);

        // 업데이트 로직이 실행되었는지 확인
        verify(dailySalesMapper, times(3)).upsertDailySales(any(DailySalesAggregation.class));
    }

    @Test
    @DisplayName("집계할 데이터가 없는 경우")
    void aggregateDailySales_NoData() {
        // Given
        given(dailySalesMapper.aggregateDailySalesByDate(targetDate))
                .willReturn(Collections.emptyList());

        // When
        int result = dailySalesAggregationService.aggregateDailySales(targetDate);

        // Then
        assertThat(result).isEqualTo(0);

        // 존재 여부 확인이나 저장 로직이 호출되지 않았는지 확인
        verify(dailySalesMapper, never())
                .existsBySellerAndProductAndDate(anyString(), anyString(), any(LocalDate.class));
        verify(dailySalesMapper, never()).upsertDailySales(any(DailySalesAggregation.class));
    }

    @Test
    @DisplayName("판매자를 찾을 수 없는 경우 - 처리 시도는 카운트됨")
    void aggregateDailySales_SellerNotFound() {
        // Given
        List<DailySalesDataDTO> dataWithInvalidSeller = Arrays.asList(
                new DailySalesDataDTO(
                        "invalid_seller", "product1", targetDate,
                        10, Long.valueOf(50000), 3
                )
        );

        given(dailySalesMapper.aggregateDailySalesByDate(targetDate))
                .willReturn(dataWithInvalidSeller);
        given(dailySalesMapper.existsBySellerAndProductAndDate(anyString(), anyString(), any(LocalDate.class)))
                .willReturn(false);
        given(dailySalesMapper.findSellerById("invalid_seller")).willReturn(null);

        // When
        int result = dailySalesAggregationService.aggregateDailySales(targetDate);

        // Then
        // 현재 구현체는 seller가 null이어도 처리 시도로 카운트함
        assertThat(result).isEqualTo(1);

        // upsert가 호출되지 않았는지 확인 (실제 저장은 안됨)
        verify(dailySalesMapper, never()).upsertDailySales(any(DailySalesAggregation.class));
    }

    @Test
    @DisplayName("상품을 찾을 수 없는 경우 - 처리 시도는 카운트됨")
    void aggregateDailySales_ProductNotFound() {
        // Given
        given(dailySalesMapper.aggregateDailySalesByDate(targetDate))
                .willReturn(Arrays.asList(sampleAggregatedData.get(0)));
        given(dailySalesMapper.existsBySellerAndProductAndDate(anyString(), anyString(), any(LocalDate.class)))
                .willReturn(false);
        given(dailySalesMapper.findSellerById("seller1")).willReturn(mockSeller);
        given(dailySalesMapper.findProductById("product1")).willReturn(null);

        // When
        int result = dailySalesAggregationService.aggregateDailySales(targetDate);

        // Then
        // 현재 구현체는 product가 null이어도 처리 시도로 카운트함
        assertThat(result).isEqualTo(1);

        // upsert가 호출되지 않았는지 확인 (실제 저장은 안됨)
        verify(dailySalesMapper, never()).upsertDailySales(any(DailySalesAggregation.class));
    }

    @Test
    @DisplayName("집계 데이터 조회 중 예외 발생")
    void aggregateDailySales_Exception_During_DataRetrieval() {
        // Given
        given(dailySalesMapper.aggregateDailySalesByDate(targetDate))
                .willThrow(new RuntimeException("데이터베이스 오류"));

        // When & Then
        assertThatThrownBy(() -> dailySalesAggregationService.aggregateDailySales(targetDate))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("일별 판매 집계 중 오류가 발생했습니다");
    }

    @Test
    @DisplayName("혼합 상황 - 일부 성공, 일부 실패")
    void aggregateDailySales_PartialSuccess() {
        // Given
        given(dailySalesMapper.aggregateDailySalesByDate(targetDate))
                .willReturn(sampleAggregatedData);

        given(dailySalesMapper.existsBySellerAndProductAndDate(anyString(), anyString(), any(LocalDate.class)))
                .willReturn(false);

        // 첫 번째와 세 번째 데이터는 성공, 두 번째는 상품 없음
        given(dailySalesMapper.findSellerById("seller1")).willReturn(mockSeller);
        given(dailySalesMapper.findSellerById("seller2")).willReturn(mockSeller);

        given(dailySalesMapper.findProductById("product1")).willReturn(mockProduct);
        given(dailySalesMapper.findProductById("product2")).willReturn(null); // 실패 케이스
        given(dailySalesMapper.findProductById("product3")).willReturn(mockProduct);

        // When
        int result = dailySalesAggregationService.aggregateDailySales(targetDate);

        // Then
        // 현재 구현체는 모든 시도를 카운트함 (실제 저장 성공 여부와 무관)
        assertThat(result).isEqualTo(3);

        // upsert가 성공한 데이터에 대해서만 호출되었는지 확인
        verify(dailySalesMapper, times(2)).upsertDailySales(any(DailySalesAggregation.class));
    }

    @Test
    @DisplayName("날짜 경계값 테스트 - 어제 날짜")
    void aggregateDailySales_Yesterday() {
        // Given
        LocalDate yesterday = LocalDate.now().minusDays(1);
        given(dailySalesMapper.aggregateDailySalesByDate(yesterday))
                .willReturn(Collections.emptyList());

        // When
        int result = dailySalesAggregationService.aggregateDailySales(yesterday);

        // Then
        assertThat(result).isEqualTo(0);
        verify(dailySalesMapper).aggregateDailySalesByDate(yesterday);
    }

    @Test
    @DisplayName("날짜 경계값 테스트 - 한 달 전 날짜")
    void aggregateDailySales_OneMonthAgo() {
        // Given
        LocalDate oneMonthAgo = LocalDate.now().minusMonths(1);
        given(dailySalesMapper.aggregateDailySalesByDate(oneMonthAgo))
                .willReturn(Collections.emptyList());

        // When
        int result = dailySalesAggregationService.aggregateDailySales(oneMonthAgo);

        // Then
        assertThat(result).isEqualTo(0);
        verify(dailySalesMapper).aggregateDailySalesByDate(oneMonthAgo);
    }
}