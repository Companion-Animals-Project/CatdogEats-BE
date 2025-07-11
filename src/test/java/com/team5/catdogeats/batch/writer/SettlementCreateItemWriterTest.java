package com.team5.catdogeats.batch.writer;

import com.team5.catdogeats.batch.dto.SettlementBatchItem;
import com.team5.catdogeats.batch.mapper.SettlementChunkMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("정산 생성 ItemWriter 단위 테스트")
class SettlementCreateItemWriterTest {

    @Mock
    private SettlementChunkMapper settlementChunkMapper;

    private SettlementCreateItemWriter writer;

    @BeforeEach
    void setUp() {
        writer = new SettlementCreateItemWriter(settlementChunkMapper);
    }

    @Test
    @DisplayName("정상적인 청크 데이터 쓰기 - 성공")
    void write_NormalChunk_Success() throws Exception {
        // Given
        List<SettlementBatchItem> items = createValidSettlementItems();
        Chunk<SettlementBatchItem> chunk = new Chunk<>(items);

        willDoNothing().given(settlementChunkMapper).insertSettlement(any(SettlementBatchItem.class));

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        verify(settlementChunkMapper, times(3)).insertSettlement(any(SettlementBatchItem.class));
        verify(settlementChunkMapper).insertSettlement(argThat(item ->
                item.getOrderNumber().equals("ORDER-001") &&
                        item.getSettlementAmount().equals(95000L)
        ));
        verify(settlementChunkMapper).insertSettlement(argThat(item ->
                item.getOrderNumber().equals("ORDER-002") &&
                        item.getSettlementAmount().equals(142500L)
        ));
        verify(settlementChunkMapper).insertSettlement(argThat(item ->
                item.getOrderNumber().equals("ORDER-003") &&
                        item.getSettlementAmount().equals(190000L)
        ));
    }

    @Test
    @DisplayName("빈 청크 처리 - 아무 작업 안함")
    void write_EmptyChunk_DoNothing() throws Exception {
        // Given
        Chunk<SettlementBatchItem> emptyChunk = new Chunk<>();

        // When
        assertThatNoException().isThrownBy(() -> writer.write(emptyChunk));

        // Then
        verify(settlementChunkMapper, never()).insertSettlement(any());
    }

    @Test
    @DisplayName("유효하지 않은 데이터 포함 - 유효성 검증 실패")
    void write_InvalidData_SkipInvalidItems() throws Exception {
        // Given
        List<SettlementBatchItem> items = List.of(
                createValidSettlementItem("seller1", "ORDER-001", 100000L),
                createInvalidSettlementItem(), // 유효하지 않은 데이터
                createValidSettlementItem("seller3", "ORDER-003", 200000L)
        );
        Chunk<SettlementBatchItem> chunk = new Chunk<>(items);

        willDoNothing().given(settlementChunkMapper).insertSettlement(any(SettlementBatchItem.class));

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        // 유효한 2개만 insert 호출되어야 함
        verify(settlementChunkMapper, times(2)).insertSettlement(any(SettlementBatchItem.class));
        verify(settlementChunkMapper).insertSettlement(argThat(item ->
                item.getOrderNumber().equals("ORDER-001")
        ));
        verify(settlementChunkMapper).insertSettlement(argThat(item ->
                item.getOrderNumber().equals("ORDER-003")
        ));
    }

    @Test
    @DisplayName("개별 아이템 저장 실패 - 계속 처리")
    void write_IndividualItemFailure_ContinueProcessing() throws Exception {
        // Given
        List<SettlementBatchItem> items = createValidSettlementItems();
        Chunk<SettlementBatchItem> chunk = new Chunk<>(items);

        // 두 번째 아이템에서 예외 발생
        willDoNothing().given(settlementChunkMapper).insertSettlement(items.get(0));
        willThrow(new RuntimeException("DB 저장 실패")).given(settlementChunkMapper).insertSettlement(items.get(1));
        willDoNothing().given(settlementChunkMapper).insertSettlement(items.get(2));

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        // 모든 아이템에 대해 insert 시도되어야 함
        verify(settlementChunkMapper, times(3)).insertSettlement(any(SettlementBatchItem.class));
    }

    @Test
    @DisplayName("모든 아이템 저장 실패")
    void write_AllItemsFailure() throws Exception {
        // Given
        List<SettlementBatchItem> items = createValidSettlementItems();
        Chunk<SettlementBatchItem> chunk = new Chunk<>(items);

        willThrow(new RuntimeException("DB 연결 실패")).given(settlementChunkMapper).insertSettlement(any(SettlementBatchItem.class));

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        verify(settlementChunkMapper, times(3)).insertSettlement(any(SettlementBatchItem.class));
    }

    @Test
    @DisplayName("단일 아이템 청크 처리")
    void write_SingleItemChunk_Success() throws Exception {
        // Given
        SettlementBatchItem singleItem = createValidSettlementItem("seller1", "ORDER-001", 100000L);
        Chunk<SettlementBatchItem> chunk = new Chunk<>(List.of(singleItem));

        willDoNothing().given(settlementChunkMapper).insertSettlement(singleItem);

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        verify(settlementChunkMapper, times(1)).insertSettlement(singleItem);
        verify(settlementChunkMapper).insertSettlement(argThat(item ->
                item.getSellerId().equals("seller1") &&
                        item.getOrderNumber().equals("ORDER-001") &&
                        item.getSettlementAmount().equals(95000L)
        ));
    }

    @Test
    @DisplayName("대용량 청크 처리")
    void write_LargeChunk_Success() throws Exception {
        // Given
        List<SettlementBatchItem> largeItems = createLargeSettlementItems(100);
        Chunk<SettlementBatchItem> chunk = new Chunk<>(largeItems);

        willDoNothing().given(settlementChunkMapper).insertSettlement(any(SettlementBatchItem.class));

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        verify(settlementChunkMapper, times(100)).insertSettlement(any(SettlementBatchItem.class));
    }

    @Test
    @DisplayName("정산 데이터 필드 검증")
    void write_ValidateSettlementFields() throws Exception {
        // Given
        SettlementBatchItem item = SettlementBatchItem.forCreate(
                "seller123",
                "orderItem456",
                "ORDER-789",
                "프리미엄 사료",
                200000L,
                new BigDecimal("0.03") // 3% 수수료
        );
        Chunk<SettlementBatchItem> chunk = new Chunk<>(List.of(item));

        willDoNothing().given(settlementChunkMapper).insertSettlement(any(SettlementBatchItem.class));

        // When
        writer.write(chunk);

        // Then
        verify(settlementChunkMapper).insertSettlement(argThat(savedItem -> {
            assertThat(savedItem.getSellerId()).isEqualTo("seller123");
            assertThat(savedItem.getOrderItemId()).isEqualTo("orderItem456");
            assertThat(savedItem.getOrderNumber()).isEqualTo("ORDER-789");
            assertThat(savedItem.getProductTitle()).isEqualTo("프리미엄 사료");
            assertThat(savedItem.getItemPrice()).isEqualTo(200000L);
            assertThat(savedItem.getCommissionRate()).isEqualTo(new BigDecimal("0.03"));
            assertThat(savedItem.getCommissionAmount()).isEqualTo(6000L); // 200000 * 0.03
            assertThat(savedItem.getSettlementAmount()).isEqualTo(194000L); // 200000 - 6000
            return true;
        }));
    }

    // === Helper Methods ===

    private List<SettlementBatchItem> createValidSettlementItems() {
        return List.of(
                createValidSettlementItem("seller1", "ORDER-001", 100000L),
                createValidSettlementItem("seller2", "ORDER-002", 150000L),
                createValidSettlementItem("seller3", "ORDER-003", 200000L)
        );
    }

    private SettlementBatchItem createValidSettlementItem(String sellerId, String orderNumber, Long itemPrice) {
        return SettlementBatchItem.forCreate(
                sellerId,
                "orderItem_" + sellerId,
                orderNumber,
                "상품명_" + sellerId,
                itemPrice,
                new BigDecimal("0.05") // 5% 수수료
        );
    }

    private SettlementBatchItem createInvalidSettlementItem() {
        // isValidForCreate()가 false를 반환하도록 하는 무효한 데이터
        return SettlementBatchItem.builder()
                .sellerId(null) // 필수 필드 누락
                .orderItemId(null)
                .orderNumber("ORDER-INVALID")
                .productTitle("무효한 상품")
                .itemPrice(0L)
                .build();
    }

    private List<SettlementBatchItem> createLargeSettlementItems(int count) {
        return java.util.stream.IntStream.range(1, count + 1)
                .mapToObj(i -> createValidSettlementItem(
                        "seller" + i,
                        "ORDER-" + String.format("%03d", i),
                        100000L + (i * 1000L)
                ))
                .toList();
    }
}