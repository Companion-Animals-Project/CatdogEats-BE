//package com.team5.catdogeats.orders.service.impl;
//
//import com.team5.catdogeats.auth.dto.UserPrincipal;
//import com.team5.catdogeats.orders.domain.dto.*;
//import com.team5.catdogeats.orders.domain.enums.SettlementStatus;
//import com.team5.catdogeats.orders.mapper.SettlementMapper;
//import com.team5.catdogeats.users.domain.dto.SellerDTO;
//import com.team5.catdogeats.users.repository.SellersRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//
//import java.time.*;
//import java.util.List;
//import java.util.NoSuchElementException;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("정산현황 서비스 단위 테스트")
//class SettlementServiceImplTest {
//
//    @Mock
//    private SettlementMapper settlementMapper;
//
//    @Mock
//    private SellersRepository sellerRepository;
//
//    @InjectMocks
//    private SettlementServiceImpl settlementService;
//
//    private UserPrincipal userPrincipal;
//    private SellerDTO sellerDTO;
//    private Pageable pageable;
//
//    @BeforeEach
//    void setUp() {
//        // 공통 테스트 데이터 설정
//        userPrincipal = new UserPrincipal("kakao", "12345");
//        sellerDTO = new SellerDTO("seller123", "카카오펫샵", "image", "123-45-67890", "010-1234-5678",
//                "seller123@email.com", "대표자명",null, null, null, false, null);
//        pageable = PageRequest.of(0, 10);
//    }
//
//    @Test
//    @DisplayName("전체 정산 리스트 조회 - 성공")
//    void getSettlementList_Success() {
//        // Given
//        String sellerId = "seller123";
//        List<SettlementItemDTO> mockSettlements = createMockSettlementItems();
//        Long totalCount = 25L;
//        SettlementSummaryDTO mockSummary = createMockSettlementSummary();
//
//        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
//                .willReturn(Optional.of(sellerDTO));
//        given(settlementMapper.findSettlementsBySellerId(eq(sellerId), anyLong(), anyInt()))
//                .willReturn(mockSettlements);
//        given(settlementMapper.countSettlementsBySellerId(sellerId))
//                .willReturn(totalCount);
//        given(settlementMapper.getSettlementSummaryBySellerId(sellerId))
//                .willReturn(mockSummary);
//
//        // When
//        SettlementListResponseDto result = settlementService.getSettlementList(userPrincipal, pageable);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.settlements()).isNotNull();
//        assertThat(result.settlements().getContent()).hasSize(3);
//        assertThat(result.settlements().getTotalElements()).isEqualTo(totalCount);
//        assertThat(result.settlements().getNumber()).isEqualTo(0);
//        assertThat(result.settlements().getSize()).isEqualTo(10);
//
//        // 요약 정보 검증
//        assertThat(result.summary()).isNotNull();
//        assertThat(result.summary().totalCount()).isEqualTo(25L);
//        assertThat(result.summary().totalSettlementAmount()).isEqualTo(1500000L);
//        assertThat(result.summary().completedAmount()).isEqualTo(1000000L);
//        assertThat(result.summary().inProgressAmount()).isEqualTo(500000L);
//
//        // Mock 호출 검증
//        verify(sellerRepository).findSellerDtoByProviderAndProviderId("kakao", "12345");
//        verify(settlementMapper).findSettlementsBySellerId(sellerId, 0L, 10);
//        verify(settlementMapper).countSettlementsBySellerId(sellerId);
//        verify(settlementMapper).getSettlementSummaryBySellerId(sellerId);
//    }
//
//    @Test
//    @DisplayName("기간별 정산 리스트 조회 - 성공")
//    void getSettlementListByPeriod_Success() {
//        // Given
//        String sellerId = "seller123";
//        LocalDate startDate = LocalDate.of(2024, 1, 1);
//        LocalDate endDate = LocalDate.of(2024, 1, 31);
//        SettlementPeriodRequestDTO periodRequest = new SettlementPeriodRequestDTO(startDate, endDate);
//
//        List<SettlementItemDTO> mockSettlements = createMockSettlementItems();
//        Long totalCount = 15L;
//        SettlementSummaryDTO mockSummary = createMockPeriodSettlementSummary();
//
//        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
//                .willReturn(Optional.of(sellerDTO));
//        given(settlementMapper.findSettlementsBySellerIdAndPeriod(eq(sellerId), eq(startDate), eq(endDate), anyLong(), anyInt()))
//                .willReturn(mockSettlements);
//        given(settlementMapper.countSettlementsBySellerIdAndPeriod(sellerId, startDate, endDate))
//                .willReturn(totalCount);
//        given(settlementMapper.getSettlementSummaryBySellerIdAndPeriod(sellerId, startDate, endDate))
//                .willReturn(mockSummary);
//
//        // When
//        SettlementListResponseDto result = settlementService.getSettlementListByPeriod(userPrincipal, periodRequest, pageable);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.settlements().getContent()).hasSize(3);
//        assertThat(result.settlements().getTotalElements()).isEqualTo(totalCount);
//
//        // 기간별 요약 정보 검증
//        assertThat(result.summary().totalCount()).isEqualTo(15L);
//        assertThat(result.summary().totalSettlementAmount()).isEqualTo(800000L);
//
//        // Mock 호출 검증
//        verify(settlementMapper).findSettlementsBySellerIdAndPeriod(sellerId, startDate, endDate, 0L, 10);
//        verify(settlementMapper).countSettlementsBySellerIdAndPeriod(sellerId, startDate, endDate);
//        verify(settlementMapper).getSettlementSummaryBySellerIdAndPeriod(sellerId, startDate, endDate);
//    }
//
//    @Test
//    @DisplayName("기간별 정산 리스트 조회 - 시작일이 종료일보다 늦은 경우 실패")
//    void getSettlementListByPeriod_InvalidDateRange_ThrowsException() {
//        // Given
//        LocalDate startDate = LocalDate.of(2024, 1, 31);
//        LocalDate endDate = LocalDate.of(2024, 1, 1);
//        SettlementPeriodRequestDTO periodRequest = new SettlementPeriodRequestDTO(startDate, endDate);
//
//        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
//                .willReturn(Optional.of(sellerDTO));
//
//        // When & Then
//        assertThatThrownBy(() -> settlementService.getSettlementListByPeriod(userPrincipal, periodRequest, pageable))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("시작일은 종료일보다 이전이어야 합니다");
//    }
//
//    @Test
//    @DisplayName("이번달 정산현황 조회 - 성공")
//    void getMonthlySettlementStatus_Success() {
//        // Given
//        String sellerId = "seller123";
//        YearMonth currentMonth = YearMonth.now();
//        MonthlySettlementStatusDto mockStatus = createMockMonthlySettlementStatus();
//
//        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
//                .willReturn(Optional.of(sellerDTO));
//        given(settlementMapper.getMonthlySettlementStatus(sellerId, currentMonth))
//                .willReturn(mockStatus);
//
//        // When
//        MonthlySettlementStatusDto result = settlementService.getMonthlySettlementStatus(userPrincipal);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.totalCount()).isEqualTo(20L);
//        assertThat(result.totalMonthlyAmount()).isEqualTo(2000000L);
//        assertThat(result.completedCount()).isEqualTo(15L);
//        assertThat(result.completedAmount()).isEqualTo(1500000L);
//        assertThat(result.inProgressCount()).isEqualTo(5L);
//        assertThat(result.inProgressAmount()).isEqualTo(500000L);
//
//        // Mock 호출 검증
//        verify(settlementMapper).getMonthlySettlementStatus(sellerId, currentMonth);
//    }
//
//    @Test
//    @DisplayName("월별 정산내역 영수증 조회 - 성공")
//    void getMonthlySettlementReceipt_Success() {
//        // Given
//        String sellerId = "seller123";
//        YearMonth targetMonth = YearMonth.of(2024, 1);
//        List<SettlementItemDTO> mockItems = createMockSettlementItems();
//        MonthlySettlementStatusDto mockSummary = createMockMonthlySettlementStatus();
//
//        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
//                .willReturn(Optional.of(sellerDTO));
//        given(settlementMapper.findMonthlySettlements(sellerId, targetMonth))
//                .willReturn(mockItems);
//        given(settlementMapper.getMonthlySettlementSummary(sellerId, targetMonth))
//                .willReturn(mockSummary);
//
//        // When
//        MonthlySettlementReceiptDto result = settlementService.getMonthlySettlementReceipt(userPrincipal, targetMonth);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.targetMonth()).isEqualTo(targetMonth);
//        assertThat(result.vendorName()).isEqualTo("카카오펫샵");
//        assertThat(result.businessNumber()).isEqualTo("123-45-67890");
//        assertThat(result.items()).hasSize(3);
//        assertThat(result.summary()).isNotNull();
//        assertThat(result.summary().totalMonthlyAmount()).isEqualTo(2000000L);
//
//        // Mock 호출 검증
//        verify(settlementMapper).findMonthlySettlements(sellerId, targetMonth);
//        verify(settlementMapper).getMonthlySettlementSummary(sellerId, targetMonth);
//    }
//
//    @Test
//    @DisplayName("판매자 검증 - 판매자를 찾을 수 없는 경우 예외 발생")
//    void validateAndGetSellerId_SellerNotFound_ThrowsException() {
//        // Given
//        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
//                .willReturn(Optional.empty());
//
//        // When & Then
//        assertThatThrownBy(() -> settlementService.getSettlementList(userPrincipal, pageable))
//                .isInstanceOf(NoSuchElementException.class)
//                .hasMessage("판매자 정보를 찾을 수 없습니다");
//
//        verify(sellerRepository).findSellerDtoByProviderAndProviderId("kakao", "12345");
//    }
//
//    @Test
//    @DisplayName("전체 정산 리스트 조회 - 빈 결과")
//    void getSettlementList_EmptyResult() {
//        // Given
//        String sellerId = "seller123";
//        List<SettlementItemDTO> emptySettlements = List.of();
//        Long totalCount = 0L;
//        SettlementSummaryDTO emptySummary = new SettlementSummaryDTO(0L, 0L, 0L, 0L);
//
//        given(sellerRepository.findSellerDtoByProviderAndProviderId(anyString(), anyString()))
//                .willReturn(Optional.of(sellerDTO));
//        given(settlementMapper.findSettlementsBySellerId(eq(sellerId), anyLong(), anyInt()))
//                .willReturn(emptySettlements);
//        given(settlementMapper.countSettlementsBySellerId(sellerId))
//                .willReturn(totalCount);
//        given(settlementMapper.getSettlementSummaryBySellerId(sellerId))
//                .willReturn(emptySummary);
//
//        // When
//        SettlementListResponseDto result = settlementService.getSettlementList(userPrincipal, pageable);
//
//        // Then
//        assertThat(result).isNotNull();
//        assertThat(result.settlements().getContent()).isEmpty();
//        assertThat(result.settlements().getTotalElements()).isEqualTo(0L);
//        assertThat(result.summary().totalCount()).isEqualTo(0L);
//        assertThat(result.summary().totalSettlementAmount()).isEqualTo(0L);
//    }
//
//    // === Helper Methods ===
//
//
//    private List<SettlementItemDTO> createMockSettlementItems() {
//        return List.of(
//                new SettlementItemDTO(
//                        "ORD001",                                    // orderNumber
//                        "프리미엄 사료",                              // productName
//                        500000L,                                     // orderAmount
//                        50000L,                                      // commission
//                        450000L,                                     // settlementAmount
//                        ZonedDateTime.of(2024, 1, 15, 10, 0, 0, 0, ZoneId.of("Asia/Seoul")),       // orderDate
//                        ZonedDateTime.of(2024, 1, 22, 15, 0, 0, 0, ZoneId.of("Asia/Seoul")),       // deliveryDate
//                        ZonedDateTime.of(2024, 1, 29, 9, 0, 0, 0, ZoneId.of("Asia/Seoul")),        // settlementCreatedAt
//                        SettlementStatus.COMPLETED                   // status
//                ),
//                new SettlementItemDTO(
//                        "ORD002",                                    // orderNumber
//                        "강아지 간식",                                // productName
//                        300000L,                                     // orderAmount
//                        30000L,                                      // commission
//                        270000L,                                     // settlementAmount
//                        ZonedDateTime.of(2024, 1, 20, 14, 30, 0, 0, ZoneId.of("Asia/Seoul")),     // orderDate
//                        ZonedDateTime.of(2024, 1, 27, 11, 15, 0, 0, ZoneId.of("Asia/Seoul")),     // deliveryDate
//                        ZonedDateTime.of(2024, 2, 3, 10, 30, 0, 0, ZoneId.of("Asia/Seoul")),      // settlementCreatedAt
//                        SettlementStatus.IN_PROGRESS                 // status
//                ),
//                new SettlementItemDTO(
//                        "ORD003",                                    // orderNumber
//                        "고양이 모래",                                // productName
//                        750000L,                                     // orderAmount
//                        75000L,                                      // commission
//                        675000L,                                     // settlementAmount
//                        ZonedDateTime.of(2024, 1, 25, 16, 45, 0, 0, ZoneId.of("Asia/Seoul")),     // orderDate
//                        ZonedDateTime.of(2024, 2, 1, 13, 20, 0, 0, ZoneId.of("Asia/Seoul")),      // deliveryDate
//                        ZonedDateTime.of(2024, 2, 8, 8, 45, 0, 0, ZoneId.of("Asia/Seoul")),       // settlementCreatedAt
//                        SettlementStatus.COMPLETED                   // status
//                )
//        );
//    }
//
//    private SettlementSummaryDTO createMockSettlementSummary() {
//        return new SettlementSummaryDTO(
//                25L,        // totalCount
//                1500000L,   // totalSettlementAmount
//                1000000L,   // completedAmount
//                500000L     // inProgressAmount
//        );
//    }
//
//    private SettlementSummaryDTO createMockPeriodSettlementSummary() {
//        return new SettlementSummaryDTO(
//                15L,        // totalCount
//                800000L,    // totalSettlementAmount
//                600000L,    // completedAmount
//                200000L     // inProgressAmount
//        );
//    }
//
//    private MonthlySettlementStatusDto createMockMonthlySettlementStatus() {
//        return new MonthlySettlementStatusDto(
//                20L,        // totalCount
//                2000000L,   // totalMonthlyAmount
//                15L,        // completedCount
//                1500000L,   // completedAmount
//                5L,         // inProgressCount
//                500000L     // inProgressAmount
//        );
//    }
//}