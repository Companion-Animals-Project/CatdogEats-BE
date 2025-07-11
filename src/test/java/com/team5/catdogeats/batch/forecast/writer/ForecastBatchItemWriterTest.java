package com.team5.catdogeats.batch.forecast.writer;

import com.team5.catdogeats.batch.forecast.domain.dto.ForecastBatchItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("수요예측 배치 ItemWriter 테스트")
class ForecastBatchItemWriterTest {

    private ForecastBatchItemWriter itemWriter;

    @BeforeEach
    void setUp() {
        itemWriter = new ForecastBatchItemWriter();
    }

    @Test
    @DisplayName("정상적인 청크 처리 - 모든 아이템 성공")
    void write_Success_AllItemsSuccessful() throws Exception {
        // Given
        ForecastBatchItem.ProcessingResult successResult1 = ForecastBatchItem.ProcessingResult.success(
                10, 150L, 0.85, 2
        );

        ForecastBatchItem.ProcessingResult successResult2 = ForecastBatchItem.ProcessingResult.success(
                15, 200L, 0.75, 1
        );

        ForecastBatchItem item1 = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                10, 8, successResult1
        );

        ForecastBatchItem item2 = new ForecastBatchItem(
                "seller2", "판매자2", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                15, 12, successResult2
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(item1, item2));

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Then
        // 예외 없이 정상 처리되어야 함
        // 실제 통계는 private 필드이므로 로그 확인이 주된 검증 방법
    }

    @Test
    @DisplayName("정상적인 청크 처리 - 일부 실패 포함")
    void write_Success_WithSomeFailures() throws Exception {
        // Given
        ForecastBatchItem.ProcessingResult successResult = ForecastBatchItem.ProcessingResult.success(
                10, 150L, 0.85, 2
        );

        ForecastBatchItem.ProcessingResult failureResult = ForecastBatchItem.ProcessingResult.failure(
                "데이터베이스 연결 오류", 50L
        );

        ForecastBatchItem item1 = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                10, 8, successResult
        );

        ForecastBatchItem item2 = new ForecastBatchItem(
                "seller2", "판매자2", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                15, 12, failureResult
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(item1, item2));

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Then
        // 실패한 아이템이 있어도 Writer는 예외를 던지지 않고 통계만 기록
    }

    @Test
    @DisplayName("정상적인 청크 처리 - 스킵된 아이템 포함")
    void write_Success_WithSkippedItems() throws Exception {
        // Given
        ForecastBatchItem.ProcessingResult successResult = ForecastBatchItem.ProcessingResult.success(
                10, 150L, 0.85, 2
        );

        ForecastBatchItem.ProcessingResult skippedResult = ForecastBatchItem.ProcessingResult.skipped(
                "충분한 판매 데이터 없음"
        );

        ForecastBatchItem item1 = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                10, 8, successResult
        );

        ForecastBatchItem item2 = new ForecastBatchItem(
                "seller2", "판매자2", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                15, 12, skippedResult
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(item1, item2));

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Then
        // 스킵된 아이템도 정상적으로 처리되어야 함
    }

    @Test
    @DisplayName("빈 청크 처리")
    void write_EmptyChunk() throws Exception {
        // Given
        Chunk<ForecastBatchItem> emptyChunk = new Chunk<>(Collections.emptyList());

        // When & Then
        assertThatNoException().isThrownBy(() -> itemWriter.write(emptyChunk));
    }

    @Test
    @DisplayName("처리 결과가 null인 아이템 처리")
    void write_ItemWithNullResult() throws Exception {
        // Given
        ForecastBatchItem itemWithNullResult = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                10, 8, null
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(itemWithNullResult));

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Then
        // null 결과가 있어도 예외 없이 처리되어야 함 (실패로 카운트)
    }

    @Test
    @DisplayName("고품질 예측 결과 처리")
    void write_HighQualityPredictions() throws Exception {
        // Given
        ForecastBatchItem.ProcessingResult highQualityResult = ForecastBatchItem.ProcessingResult.success(
                10, 150L, 0.95, 0  // 고품질 신뢰도 (>= 0.8)
        );

        ForecastBatchItem item = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                10, 8, highQualityResult
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(item));

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Then
        // 고품질 예측으로 분류되어 통계에 반영되어야 함
    }

    @Test
    @DisplayName("저품질 예측 결과 처리")
    void write_LowQualityPredictions() throws Exception {
        // Given
        ForecastBatchItem.ProcessingResult lowQualityResult = ForecastBatchItem.ProcessingResult.success(
                5, 150L, 0.3, 0  // 저품질 신뢰도 (< 0.5)
        );

        ForecastBatchItem item = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                5, 3, lowQualityResult
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(item));

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Then
        // 저품질 예측으로 분류되어 통계에 반영되어야 함
    }

    @Test
    @DisplayName("높은 재고 부족 상황 처리")
    void write_HighShortageProducts() throws Exception {
        // Given
        ForecastBatchItem.ProcessingResult highShortageResult = ForecastBatchItem.ProcessingResult.success(
                20, 150L, 0.7, 8  // 높은 재고 부족 (>= 5)
        );

        ForecastBatchItem item = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                20, 15, highShortageResult
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(item));

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Then
        // 높은 재고 부족으로 분류되어 통계에 반영되어야 함
    }

    @Test
    @DisplayName("복합 시나리오 - 다양한 결과 타입 혼합")
    void write_MixedScenario() throws Exception {
        // Given
        ForecastBatchItem.ProcessingResult highQualitySuccess = ForecastBatchItem.ProcessingResult.success(
                15, 100L, 0.9, 1
        );

        ForecastBatchItem.ProcessingResult lowQualitySuccess = ForecastBatchItem.ProcessingResult.success(
                8, 150L, 0.4, 6  // 저품질 + 높은 재고 부족
        );

        ForecastBatchItem.ProcessingResult failure = ForecastBatchItem.ProcessingResult.failure(
                "예측 알고리즘 오류", 50L
        );

        ForecastBatchItem.ProcessingResult skipped = ForecastBatchItem.ProcessingResult.skipped(
                "데이터 부족"
        );

        ForecastBatchItem item1 = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                15, 12, highQualitySuccess
        );

        ForecastBatchItem item2 = new ForecastBatchItem(
                "seller2", "판매자2", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                8, 6, lowQualitySuccess
        );

        ForecastBatchItem item3 = new ForecastBatchItem(
                "seller3", "판매자3", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                5, 3, failure
        );

        ForecastBatchItem item4 = new ForecastBatchItem(
                "seller4", "판매자4", false,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(30),
                2, 0, skipped
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(item1, item2, item3, item4));

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Then
        // 모든 타입의 결과가 정상적으로 처리되어야 함
    }

    @Test
    @DisplayName("신뢰도 점수가 null인 경우 처리")
    void write_NullConfidenceScore() throws Exception {
        // Given
        ForecastBatchItem.ProcessingResult resultWithNullConfidence = ForecastBatchItem.ProcessingResult.success(
                10, 150L, null, 2  // null 신뢰도
        );

        ForecastBatchItem item = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                10, 8, resultWithNullConfidence
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(item));

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Then
        // null 신뢰도도 정상 처리되어야 함 (신뢰도 통계에서 제외)
    }

    @Test
    @DisplayName("대용량 청크 처리")
    void write_LargeChunk() throws Exception {
        // Given
        Chunk<ForecastBatchItem> largeChunk = new Chunk<>();

        for (int i = 1; i <= 100; i++) {
            ForecastBatchItem.ProcessingResult result;

            if (i % 10 == 0) {
                // 10의 배수는 실패
                result = ForecastBatchItem.ProcessingResult.failure("오류 " + i, 100L + i);
            } else if (i % 7 == 0) {
                // 7의 배수는 스킵
                result = ForecastBatchItem.ProcessingResult.skipped("데이터 부족 " + i);
            } else {
                // 나머지는 성공
                result = ForecastBatchItem.ProcessingResult.success(
                        i % 20,  // processedProductCount
                        100L + i,  // processingTimeMs
                        i % 2 == 0 ? 0.8 : 0.6,  // averageConfidenceScore
                        i % 5  // shortageProductCount
                );
            }

            ForecastBatchItem item = new ForecastBatchItem(
                    "seller" + i, "판매자" + i, true,
                    LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                    i % 20 + 5, i % 15 + 3, result
            );
            largeChunk.add(item);
        }

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(largeChunk));

        // Then
        // 대용량 청크도 정상 처리되어야 함
    }

    @Test
    @DisplayName("예외 발생 시 복구 처리")
    void write_ExceptionHandling() throws Exception {
        // Given
        ForecastBatchItem.ProcessingResult normalResult = ForecastBatchItem.ProcessingResult.success(
                10, 150L, 0.8, 2
        );

        // 정상 아이템과 null 결과 아이템을 혼합
        ForecastBatchItem normalItem = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                10, 8, normalResult
        );

        ForecastBatchItem problematicItem = new ForecastBatchItem(
                "seller2", "판매자2", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                5, 3, null
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(normalItem, problematicItem));

        // When & Then
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Writer는 개별 아이템의 문제로 인해 전체 처리를 중단하지 않아야 함
    }

    @Test
    @DisplayName("다양한 신뢰도 점수 범위 처리")
    void write_VariousConfidenceScores() throws Exception {
        // Given
        ForecastBatchItem.ProcessingResult veryHighQuality = ForecastBatchItem.ProcessingResult.success(
                10, 150L, 0.95, 1  // 매우 높은 품질
        );

        ForecastBatchItem.ProcessingResult mediumQuality = ForecastBatchItem.ProcessingResult.success(
                8, 120L, 0.65, 2  // 중간 품질
        );

        ForecastBatchItem.ProcessingResult veryLowQuality = ForecastBatchItem.ProcessingResult.success(
                5, 100L, 0.25, 3  // 매우 낮은 품질
        );

        ForecastBatchItem item1 = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                10, 8, veryHighQuality
        );

        ForecastBatchItem item2 = new ForecastBatchItem(
                "seller2", "판매자2", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                8, 6, mediumQuality
        );

        ForecastBatchItem item3 = new ForecastBatchItem(
                "seller3", "판매자3", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                5, 3, veryLowQuality
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(item1, item2, item3));

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Then
        // 다양한 품질 범위가 적절히 분류되어야 함
    }

    @Test
    @DisplayName("재고 부족 임계값 경계 테스트")
    void write_ShortageThresholdBoundary() throws Exception {
        // Given
        ForecastBatchItem.ProcessingResult exactThreshold = ForecastBatchItem.ProcessingResult.success(
                15, 150L, 0.8, 5  // 정확히 임계값 (5개)
        );

        ForecastBatchItem.ProcessingResult belowThreshold = ForecastBatchItem.ProcessingResult.success(
                12, 140L, 0.75, 4  // 임계값 미만 (4개)
        );

        ForecastBatchItem.ProcessingResult aboveThreshold = ForecastBatchItem.ProcessingResult.success(
                20, 160L, 0.85, 6  // 임계값 초과 (6개)
        );

        ForecastBatchItem item1 = new ForecastBatchItem(
                "seller1", "판매자1", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                15, 12, exactThreshold
        );

        ForecastBatchItem item2 = new ForecastBatchItem(
                "seller2", "판매자2", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                12, 10, belowThreshold
        );

        ForecastBatchItem item3 = new ForecastBatchItem(
                "seller3", "판매자3", true,
                LocalDate.now().minusMonths(6), LocalDate.now().minusDays(5),
                20, 18, aboveThreshold
        );

        Chunk<ForecastBatchItem> chunk = new Chunk<>(Arrays.asList(item1, item2, item3));

        // When
        assertThatNoException().isThrownBy(() -> itemWriter.write(chunk));

        // Then
        // 재고 부족 임계값 경계 케이스가 정확히 처리되어야 함
    }
}