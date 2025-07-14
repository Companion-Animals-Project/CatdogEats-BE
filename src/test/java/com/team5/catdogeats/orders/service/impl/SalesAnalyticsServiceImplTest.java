package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.dto.*;
import com.team5.catdogeats.orders.domain.enums.SalesAnalyticsType;
import com.team5.catdogeats.orders.mapper.SalesAnalyticsMapper;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.repository.SellersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("매출분석 서비스 단위 테스트")
class SalesAnalyticsServiceImplTest {

    @Mock
    private SalesAnalyticsMapper salesAnalyticsMapper;

    @Mock
    private SellersRepository sellerRepository;

    @InjectMocks
    private SalesAnalyticsServiceImpl salesAnalyticsService;

    private UserPrincipal userPrincipal;
    private SellerDTO sellerDTO;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // 공통 테스트 데이터 설정
        userPrincipal = new UserPrincipal("kakao", "12345");
        sellerDTO = new SellerDTO("seller123", "카카오펫샵", "image", "123-45-67890", "010-1234-5678",
                "seller123@email.com", "대표자명",null, null, null, false, null);
        pageable = PageRequest.of(0, 10);
    }

    @Test
    @DisplayName("기간별 매출 분석 조회 - 성공")
    void getPeriodSalesAnalytics_Success() {
        // Given
        String sellerId = "seller123";
        Integer year = 2024;

        MonthlySalesRawDataDTO yearTotalSales = new MonthlySalesRawDataDTO(
                null, 12000000L, 150L, 500L
        );

        List<MonthlySalesRawDataDTO> monthlyRawData = createMockMonthlyRawData();

        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
                .willReturn(Optional.of(sellerDTO));
        given(salesAnalyticsMapper.findYearTotalSalesBySellerAndYear(sellerId, year))
                .willReturn(yearTotalSales);
        given(salesAnalyticsMapper.findMonthlySalesBySellerAndYear(sellerId, year))
                .willReturn(monthlyRawData);

        // When
        PeriodSalesAnalyticsResponseDTO result = salesAnalyticsService.getPeriodSalesAnalytics(userPrincipal, year);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.year()).isEqualTo(year);
        assertThat(result.yearTotalAmount()).isEqualTo(12000000L);
        assertThat(result.yearTotalOrderCount()).isEqualTo(150);
        assertThat(result.yearTotalQuantity()).isEqualTo(500);
        assertThat(result.monthlyData()).hasSize(3);

        // 월별 데이터 검증
        List<MonthlySalesDataDTO> monthlyData = result.monthlyData();
        assertThat(monthlyData.get(0).month()).isEqualTo(1);
        assertThat(monthlyData.get(0).totalAmount()).isEqualTo(5000000L);
        assertThat(monthlyData.get(0).orderCount()).isEqualTo(60);
        assertThat(monthlyData.get(0).totalQuantity()).isEqualTo(200);

        // Mock 호출 검증
        verify(sellerRepository).findSellerDtoByProviderAndProviderId("kakao", "12345");
        verify(salesAnalyticsMapper).findYearTotalSalesBySellerAndYear(sellerId, year);
        verify(salesAnalyticsMapper).findMonthlySalesBySellerAndYear(sellerId, year);
    }

    @Test
    @DisplayName("상품별 매출 분석 조회 - 연도별 성공")
    void getProductSalesAnalytics_Yearly_Success() {
        // Given
        String sellerId = "seller123";
        Integer year = 2024;
        ProductSalesAnalyticsRequestDTO request = new ProductSalesAnalyticsRequestDTO(SalesAnalyticsType.YEARLY, year, null);

        List<ProductSalesRawDataDTO> rawData = createMockProductSalesRawData();
        Long totalElements = 25L;
        TotalSalesAmountDTO totalSalesAmount = new TotalSalesAmountDTO(10000000L);

        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
                .willReturn(Optional.of(sellerDTO));
        given(salesAnalyticsMapper.findYearlyProductSalesBySellerAndYear(eq(sellerId), eq(year), anyLong(), anyInt()))
                .willReturn(rawData);
        given(salesAnalyticsMapper.countYearlyProductsBySellerAndYear(sellerId, year))
                .willReturn(totalElements);
        given(salesAnalyticsMapper.findYearlyTotalAmountBySellerAndYear(sellerId, year))
                .willReturn(totalSalesAmount);

        // When
        ProductSalesAnalyticsResponseDTO result = salesAnalyticsService.getProductSalesAnalytics(userPrincipal, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("yearly");
        assertThat(result.year()).isEqualTo(year);
        assertThat(result.month()).isNull();
        assertThat(result.totalAmount()).isEqualTo(10000000L);

        // 페이징 응답 검증
        ProductSalesPageResponseDTO pageResponse = result.products();
        assertThat(pageResponse).isNotNull();
        assertThat(pageResponse.content()).hasSize(3);
        assertThat(pageResponse.totalElements()).isEqualTo(totalElements);
        assertThat(pageResponse.size()).isEqualTo(10);
        assertThat(pageResponse.number()).isEqualTo(0);
        assertThat(pageResponse.totalPages()).isEqualTo(3);
        assertThat(pageResponse.first()).isTrue();
        assertThat(pageResponse.last()).isFalse();

        // 상품별 데이터 검증 (퍼센트 계산 포함)
        List<ProductSalesDataDTO> productData = pageResponse.content();
        assertThat(productData.get(0).productName()).isEqualTo("프리미엄 사료");
        assertThat(productData.get(0).totalAmount()).isEqualTo(5000000L);
        assertThat(productData.get(0).percentage()).isEqualTo(50.0); // 5000000 / 10000000 * 100

        // Mock 호출 검증
        verify(salesAnalyticsMapper).findYearlyProductSalesBySellerAndYear(sellerId, year, 0L, 10);
        verify(salesAnalyticsMapper).countYearlyProductsBySellerAndYear(sellerId, year);
        verify(salesAnalyticsMapper).findYearlyTotalAmountBySellerAndYear(sellerId, year);
    }

    @Test
    @DisplayName("상품별 매출 분석 조회 - 월별 성공")
    void getProductSalesAnalytics_Monthly_Success() {
        // Given
        String sellerId = "seller123";
        Integer year = 2024;
        Integer month = 1;
        ProductSalesAnalyticsRequestDTO request = new ProductSalesAnalyticsRequestDTO(SalesAnalyticsType.MONTHLY, year, month);

        List<ProductSalesRawDataDTO> rawData = createMockProductSalesRawData();
        Long totalElements = 15L;
        TotalSalesAmountDTO totalSalesAmount = new TotalSalesAmountDTO(8000000L);

        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
                .willReturn(Optional.of(sellerDTO));
        given(salesAnalyticsMapper.findMonthlyProductSalesBySellerAndYearMonth(eq(sellerId), eq(year), eq(month), anyLong(), anyInt()))
                .willReturn(rawData);
        given(salesAnalyticsMapper.countMonthlyProductsBySellerAndYearMonth(sellerId, year, month))
                .willReturn(totalElements);
        given(salesAnalyticsMapper.findMonthlyTotalAmountBySellerAndYearMonth(sellerId, year, month))
                .willReturn(totalSalesAmount);

        // When
        ProductSalesAnalyticsResponseDTO result = salesAnalyticsService.getProductSalesAnalytics(userPrincipal, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("monthly");
        assertThat(result.year()).isEqualTo(year);
        assertThat(result.month()).isEqualTo(month);
        assertThat(result.totalAmount()).isEqualTo(8000000L);

        // 페이징 응답 검증
        ProductSalesPageResponseDTO pageResponse = result.products();
        assertThat(pageResponse.totalElements()).isEqualTo(totalElements);
        assertThat(pageResponse.totalPages()).isEqualTo(2); // 15 / 10 = 2페이지

        // Mock 호출 검증
        verify(salesAnalyticsMapper).findMonthlyProductSalesBySellerAndYearMonth(sellerId, year, month, 0L, 10);
        verify(salesAnalyticsMapper).countMonthlyProductsBySellerAndYearMonth(sellerId, year, month);
        verify(salesAnalyticsMapper).findMonthlyTotalAmountBySellerAndYearMonth(sellerId, year, month);
    }


    @Test
    @DisplayName("퍼센트 계산 로직 검증 - 총 매출이 0인 경우")
    void calculatePercentageAndConvert_ZeroTotalAmount() {
        // Given
        String sellerId = "seller123";
        Integer year = 2024;
        ProductSalesAnalyticsRequestDTO request = new ProductSalesAnalyticsRequestDTO(SalesAnalyticsType.YEARLY, year, null);

        List<ProductSalesRawDataDTO> rawData = createMockProductSalesRawData();
        Long totalElements = 3L;
        TotalSalesAmountDTO totalSalesAmount = new TotalSalesAmountDTO(0L); // 총 매출이 0

        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
                .willReturn(Optional.of(sellerDTO));
        given(salesAnalyticsMapper.findYearlyProductSalesBySellerAndYear(eq(sellerId), eq(year), anyLong(), anyInt()))
                .willReturn(rawData);
        given(salesAnalyticsMapper.countYearlyProductsBySellerAndYear(sellerId, year))
                .willReturn(totalElements);
        given(salesAnalyticsMapper.findYearlyTotalAmountBySellerAndYear(sellerId, year))
                .willReturn(totalSalesAmount);

        // When
        ProductSalesAnalyticsResponseDTO result = salesAnalyticsService.getProductSalesAnalytics(userPrincipal, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalAmount()).isEqualTo(0L);

        // 퍼센트가 모두 0.0으로 설정되어야 함
        List<ProductSalesDataDTO> productData = result.products().content();
        assertThat(productData).allSatisfy(data -> assertThat(data.percentage()).isEqualTo(0.0));
    }

    @Test
    @DisplayName("퍼센트 계산 로직 검증 - 정상 계산")
    void calculatePercentageAndConvert_NormalCalculation() {
        // Given
        String sellerId = "seller123";
        Integer year = 2024;
        ProductSalesAnalyticsRequestDTO request = new ProductSalesAnalyticsRequestDTO(SalesAnalyticsType.YEARLY, year, null);

        List<ProductSalesRawDataDTO> rawData = List.of(
                new ProductSalesRawDataDTO("product1", "상품1", 6000000L, 60L),
                new ProductSalesRawDataDTO("product2", "상품2", 3000000L, 30L),
                new ProductSalesRawDataDTO("product3", "상품3", 1000000L, 10L)
        );
        Long totalElements = 3L;
        TotalSalesAmountDTO totalSalesAmount = new TotalSalesAmountDTO(10000000L);

        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
                .willReturn(Optional.of(sellerDTO));
        given(salesAnalyticsMapper.findYearlyProductSalesBySellerAndYear(eq(sellerId), eq(year), anyLong(), anyInt()))
                .willReturn(rawData);
        given(salesAnalyticsMapper.countYearlyProductsBySellerAndYear(sellerId, year))
                .willReturn(totalElements);
        given(salesAnalyticsMapper.findYearlyTotalAmountBySellerAndYear(sellerId, year))
                .willReturn(totalSalesAmount);

        // When
        ProductSalesAnalyticsResponseDTO result = salesAnalyticsService.getProductSalesAnalytics(userPrincipal, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalAmount()).isEqualTo(10000000L);

        // 퍼센트 계산 정확성 검증
        List<ProductSalesDataDTO> productData = result.products().content();
        assertThat(productData.get(0).percentage()).isEqualTo(60.0); // 6000000 / 10000000 * 100
        assertThat(productData.get(1).percentage()).isEqualTo(30.0); // 3000000 / 10000000 * 100
        assertThat(productData.get(2).percentage()).isEqualTo(10.0); // 1000000 / 10000000 * 100
    }

    @Test
    @DisplayName("판매자 검증 - 판매자를 찾을 수 없는 경우 예외 발생")
    void validateAndGetSellerId_SellerNotFound_ThrowsException() {
        // Given
        Integer year = 2024;
        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
                .willReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> salesAnalyticsService.getPeriodSalesAnalytics(userPrincipal, year))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("판매자 정보를 찾을 수 없습니다");

        verify(sellerRepository).findSellerDtoByProviderAndProviderId("kakao", "12345");
    }

    @Test
    @DisplayName("기간별 매출 분석 조회 - 데이터 없음")
    void getPeriodSalesAnalytics_NoData() {
        // Given
        String sellerId = "seller123";
        Integer year = 2024;

        MonthlySalesRawDataDTO yearTotalSales = new MonthlySalesRawDataDTO(null, 0L, 0L, 0L);
        List<MonthlySalesRawDataDTO> emptyMonthlyData = List.of();

        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
                .willReturn(Optional.of(sellerDTO));
        given(salesAnalyticsMapper.findYearTotalSalesBySellerAndYear(sellerId, year))
                .willReturn(yearTotalSales);
        given(salesAnalyticsMapper.findMonthlySalesBySellerAndYear(sellerId, year))
                .willReturn(emptyMonthlyData);

        // When
        PeriodSalesAnalyticsResponseDTO result = salesAnalyticsService.getPeriodSalesAnalytics(userPrincipal, year);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.year()).isEqualTo(year);
        assertThat(result.yearTotalAmount()).isEqualTo(0L);
        assertThat(result.yearTotalOrderCount()).isEqualTo(0);
        assertThat(result.yearTotalQuantity()).isEqualTo(0);
        assertThat(result.monthlyData()).isEmpty();
    }

    @Test
    @DisplayName("상품별 매출 분석 조회 - 빈 결과")
    void getProductSalesAnalytics_EmptyResult() {
        // Given
        String sellerId = "seller123";
        Integer year = 2024;
        ProductSalesAnalyticsRequestDTO request = new ProductSalesAnalyticsRequestDTO(SalesAnalyticsType.YEARLY, year, null);

        List<ProductSalesRawDataDTO> emptyRawData = List.of();
        Long totalElements = 0L;
        TotalSalesAmountDTO totalSalesAmount = new TotalSalesAmountDTO(0L);

        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
                .willReturn(Optional.of(sellerDTO));
        given(salesAnalyticsMapper.findYearlyProductSalesBySellerAndYear(eq(sellerId), eq(year), anyLong(), anyInt()))
                .willReturn(emptyRawData);
        given(salesAnalyticsMapper.countYearlyProductsBySellerAndYear(sellerId, year))
                .willReturn(totalElements);
        given(salesAnalyticsMapper.findYearlyTotalAmountBySellerAndYear(sellerId, year))
                .willReturn(totalSalesAmount);

        // When
        ProductSalesAnalyticsResponseDTO result = salesAnalyticsService.getProductSalesAnalytics(userPrincipal, request, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.totalAmount()).isEqualTo(0L);

        // 페이징 응답 검증
        ProductSalesPageResponseDTO pageResponse = result.products();
        assertThat(pageResponse.content()).isEmpty();
        assertThat(pageResponse.totalElements()).isEqualTo(0L);
        assertThat(pageResponse.totalPages()).isEqualTo(0);
        assertThat(pageResponse.first()).isTrue();
        assertThat(pageResponse.last()).isTrue();
    }

    @Test
    @DisplayName("페이징 응답 생성 로직 검증")
    void createPageResponse_CorrectPagination() {
        // Given
        String sellerId = "seller123";
        Integer year = 2024;
        ProductSalesAnalyticsRequestDTO request = new ProductSalesAnalyticsRequestDTO(SalesAnalyticsType.YEARLY, year, null);
        Pageable secondPage = PageRequest.of(1, 10); // 2페이지

        List<ProductSalesRawDataDTO> rawData = createMockProductSalesRawData();
        Long totalElements = 25L; // 총 25개 데이터
        TotalSalesAmountDTO totalSalesAmount = new TotalSalesAmountDTO(10000000L);

        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
                .willReturn(Optional.of(sellerDTO));
        given(salesAnalyticsMapper.findYearlyProductSalesBySellerAndYear(eq(sellerId), eq(year), anyLong(), anyInt()))
                .willReturn(rawData);
        given(salesAnalyticsMapper.countYearlyProductsBySellerAndYear(sellerId, year))
                .willReturn(totalElements);
        given(salesAnalyticsMapper.findYearlyTotalAmountBySellerAndYear(sellerId, year))
                .willReturn(totalSalesAmount);

        // When
        ProductSalesAnalyticsResponseDTO result = salesAnalyticsService.getProductSalesAnalytics(userPrincipal, request, secondPage);

        // Then
        ProductSalesPageResponseDTO pageResponse = result.products();
        assertThat(pageResponse.totalElements()).isEqualTo(25L);
        assertThat(pageResponse.size()).isEqualTo(10);
        assertThat(pageResponse.number()).isEqualTo(1); // 2페이지 (0-based)
        assertThat(pageResponse.totalPages()).isEqualTo(3); // 25 / 10 = 3페이지
        assertThat(pageResponse.first()).isFalse(); // 첫 번째 페이지 아님
        assertThat(pageResponse.last()).isFalse(); // 마지막 페이지 아님

        // Mock 호출 검증 (offset 계산 확인)
        verify(salesAnalyticsMapper).findYearlyProductSalesBySellerAndYear(sellerId, year, 10L, 10);
    }

    // === Helper Methods ===

    private List<MonthlySalesRawDataDTO> createMockMonthlyRawData() {
        return List.of(
                new MonthlySalesRawDataDTO(1, 5000000L, 60L, 200L),
                new MonthlySalesRawDataDTO(2, 4000000L, 50L, 180L),
                new MonthlySalesRawDataDTO(3, 3000000L, 40L, 120L)
        );
    }

    private List<ProductSalesRawDataDTO> createMockProductSalesRawData() {
        return List.of(
                new ProductSalesRawDataDTO("product1", "프리미엄 사료", 5000000L, 100L),
                new ProductSalesRawDataDTO("product2", "고급 간식", 3000000L, 75L),
                new ProductSalesRawDataDTO("product3", "장난감", 2000000L, 50L)
        );
    }
}