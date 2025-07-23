//package com.team5.catdogeats.batch.forecast.reader;
//
//import com.team5.catdogeats.batch.dto.ForecastBatchItem;
//import com.team5.catdogeats.batch.reader.ForecastBatchItemReader;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.time.LocalDate;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.BDDMockito.*;
//
//@ExtendWith(MockitoExtension.class)
//@DisplayName("수요예측 배치 ItemReader 테스트")
//class ForecastBatchItemReaderTest {
//
//    @Mock
//    private ForecastBatchMapper forecastBatchMapper;
//
//    private ForecastBatchItemReader itemReader;
//    private final int chunkSize = 3;
//
//    private List<ForecastBatchItem> firstChunk;
//    private List<ForecastBatchItem> secondChunk;
//    private List<ForecastBatchItem> thirdChunk;
//
//    @BeforeEach
//    void setUp() {
//        itemReader = new ForecastBatchItemReader(forecastBatchMapper, chunkSize);
//
//        // 테스트용 배치 아이템 생성 - 8개 필드 모두 설정
//        firstChunk = Arrays.asList(
//                new ForecastBatchItem("seller1", "판매자1", true,
//                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
//                        10, 8, null),
//                new ForecastBatchItem("seller2", "판매자2", true,
//                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
//                        10, 8, null),
//                new ForecastBatchItem("seller3", "판매자3", true,
//                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
//                        10, 8, null)
//        );
//
//        secondChunk = Arrays.asList(
//                new ForecastBatchItem("seller4", "판매자4", true,
//                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
//                        10, 8, null),
//                new ForecastBatchItem("seller5", "판매자5", true,
//                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
//                        10, 8, null)
//        );
//
//        thirdChunk = Collections.emptyList();
//    }
//
//
//
//    @Test
//    @DisplayName("단일 청크 데이터 읽기")
//    void read_Success_SingleChunk() throws Exception {
//        // Given
//        List<ForecastBatchItem> singleChunk = Arrays.asList(
//                new ForecastBatchItem("seller1", "판매자1", true,
//                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
//                        10, 8, null),
//                new ForecastBatchItem("seller2", "판매자2", true,
//                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
//                        10, 8, null)
//        );
//
//        given(forecastBatchMapper.countSellersForForecastBatch()).willReturn(2L);
//        given(forecastBatchMapper.findSellersForForecastBatch(any(LocalDate.class), eq(chunkSize), eq(0)))
//                .willReturn(singleChunk);
//
//        // When & Then
//        ForecastBatchItem item1 = itemReader.read();
//        assertThat(item1).isNotNull();
//        assertThat(item1.sellerId()).isEqualTo("seller1");
//
//        ForecastBatchItem item2 = itemReader.read();
//        assertThat(item2).isNotNull();
//        assertThat(item2.sellerId()).isEqualTo("seller2");
//
//        ForecastBatchItem nullItem = itemReader.read();
//        assertThat(nullItem).isNull();
//
//        // 검증 - 두 번째 청크 조회는 하지 않아야 함 (첫 번째 청크 크기가 chunkSize보다 작음)
//        verify(forecastBatchMapper).findSellersForForecastBatch(any(LocalDate.class), eq(chunkSize), eq(0));
//        verify(forecastBatchMapper, never()).findSellersForForecastBatch(any(LocalDate.class), eq(chunkSize), eq(2));
//    }
//
//    @Test
//    @DisplayName("처리할 데이터가 없는 경우")
//    void read_NoData() throws Exception {
//        // Given
//        given(forecastBatchMapper.countSellersForForecastBatch()).willReturn(0L);
//
//        // When
//        ForecastBatchItem item = itemReader.read();
//
//        // Then
//        assertThat(item).isNull();
//
//        // count만 호출되고 실제 데이터 조회는 하지 않아야 함
//        verify(forecastBatchMapper).countSellersForForecastBatch();
//        verify(forecastBatchMapper, never()).findSellersForForecastBatch(any(LocalDate.class), anyInt(), anyInt());
//    }
//
//    @Test
//    @DisplayName("초기화 중 예외 발생")
//    void read_InitializationException() throws Exception {
//        // Given
//        given(forecastBatchMapper.countSellersForForecastBatch())
//                .willThrow(new RuntimeException("데이터베이스 오류"));
//
//        // When & Then
//        assertThatThrownBy(() -> itemReader.read())
//                .isInstanceOf(RuntimeException.class)
//                .hasMessage("ItemReader 초기화 중 오류 발생");
//    }
//
//
//
//    @Test
//    @DisplayName("정확한 청크 크기로 데이터 읽기")
//    void read_ExactChunkSize() throws Exception {
//        // Given - 정확히 chunkSize만큼의 데이터
//        given(forecastBatchMapper.countSellersForForecastBatch()).willReturn(3L);
//        given(forecastBatchMapper.findSellersForForecastBatch(any(LocalDate.class), eq(chunkSize), eq(0)))
//                .willReturn(firstChunk); // 정확히 3개
//        given(forecastBatchMapper.findSellersForForecastBatch(any(LocalDate.class), eq(chunkSize), eq(3)))
//                .willReturn(Collections.emptyList());
//
//        // When & Then
//        // 3개 아이템 읽기
//        for (int i = 0; i < 3; i++) {
//            ForecastBatchItem item = itemReader.read();
//            assertThat(item).isNotNull();
//            assertThat(item.sellerId()).isEqualTo("seller" + (i + 1));
//        }
//
//        // 더 이상 데이터 없음
//        ForecastBatchItem nullItem = itemReader.read();
//        assertThat(nullItem).isNull();
//
//        // 다음 청크 조회가 시도되었는지 확인
//        verify(forecastBatchMapper).findSellersForForecastBatch(any(LocalDate.class), eq(chunkSize), eq(3));
//    }
//
//
//
//    @Test
//    @DisplayName("청크 크기보다 작은 데이터 처리")
//    void read_DataSmallerThanChunkSize() throws Exception {
//        // Given - 청크 크기(3)보다 작은 데이터(2개)
//        List<ForecastBatchItem> smallChunk = Arrays.asList(
//                new ForecastBatchItem("seller1", "판매자1", true,
//                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
//                        10, 8, null),
//                new ForecastBatchItem("seller2", "판매자2", true,
//                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
//                        10, 8, null)
//        );
//
//        given(forecastBatchMapper.countSellersForForecastBatch()).willReturn(2L);
//        given(forecastBatchMapper.findSellersForForecastBatch(any(LocalDate.class), eq(chunkSize), eq(0)))
//                .willReturn(smallChunk);
//
//        // When & Then
//        ForecastBatchItem item1 = itemReader.read();
//        assertThat(item1).isNotNull();
//        assertThat(item1.sellerId()).isEqualTo("seller1");
//
//        ForecastBatchItem item2 = itemReader.read();
//        assertThat(item2).isNotNull();
//        assertThat(item2.sellerId()).isEqualTo("seller2");
//
//        ForecastBatchItem nullItem = itemReader.read();
//        assertThat(nullItem).isNull();
//
//        // 첫 번째 청크만 조회되어야 함 (크기가 chunkSize보다 작으므로 마지막 청크로 인식)
//        verify(forecastBatchMapper, times(1)).findSellersForForecastBatch(any(LocalDate.class), anyInt(), anyInt());
//    }
//
//    @Test
//    @DisplayName("여러 번 read() 호출 후 null 반환 확인")
//    void read_MultipleNullReturns() throws Exception {
//        // Given
//        given(forecastBatchMapper.countSellersForForecastBatch()).willReturn(1L);
//        given(forecastBatchMapper.findSellersForForecastBatch(any(LocalDate.class), eq(chunkSize), eq(0)))
//                .willReturn(Arrays.asList(new ForecastBatchItem("seller1", "판매자1", true,
//                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
//                        10, 8, null)));
//
//        // When & Then
//        ForecastBatchItem item = itemReader.read();
//        assertThat(item).isNotNull();
//
//        // 여러 번 null 반환 확인
//        for (int i = 0; i < 5; i++) {
//            ForecastBatchItem nullItem = itemReader.read();
//            assertThat(nullItem).isNull();
//        }
//    }
//
//    @Test
//    @DisplayName("30일 임계값 날짜 계산 확인")
//    void read_ThirtyDayThresholdCalculation() throws Exception {
//        // Given
//        given(forecastBatchMapper.countSellersForForecastBatch()).willReturn(1L);
//        given(forecastBatchMapper.findSellersForForecastBatch(any(LocalDate.class), eq(chunkSize), eq(0)))
//                .willReturn(Arrays.asList(new ForecastBatchItem("seller1", "판매자1", true,
//                        LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
//                        10, 8, null)));
//
//        // When
//        itemReader.read();
//
//        // Then - 30일 전 날짜가 올바르게 계산되어 전달되었는지 확인
//        LocalDate expectedThreshold = LocalDate.now().minusDays(30);
//        verify(forecastBatchMapper).findSellersForForecastBatch(eq(expectedThreshold), eq(chunkSize), eq(0));
//    }
//}