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

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("정산 완료 ItemWriter 단위 테스트")
class SettlementCompleteItemWriterTest {

    @Mock
    private SettlementChunkMapper settlementChunkMapper;

    private SettlementCompleteItemWriter writer;

    @BeforeEach
    void setUp() {
        writer = new SettlementCompleteItemWriter(settlementChunkMapper);
    }

    @Test
    @DisplayName("정상적인 청크 데이터 완료 처리 - 성공")
    void write_NormalChunk_Success() throws Exception {
        // Given
        List<SettlementBatchItem> items = createValidCompleteItems();
        Chunk<SettlementBatchItem> chunk = new Chunk<>(items);

        given(settlementChunkMapper.updateToCompleted("settlement-001")).willReturn(1);
        given(settlementChunkMapper.updateToCompleted("settlement-002")).willReturn(1);
        given(settlementChunkMapper.updateToCompleted("settlement-003")).willReturn(1);

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        verify(settlementChunkMapper, times(3)).updateToCompleted(anyString());
        verify(settlementChunkMapper).updateToCompleted("settlement-001");
        verify(settlementChunkMapper).updateToCompleted("settlement-002");
        verify(settlementChunkMapper).updateToCompleted("settlement-003");
    }

    @Test
    @DisplayName("빈 청크 처리 - 아무 작업 안함")
    void write_EmptyChunk_DoNothing() throws Exception {
        // Given
        Chunk<SettlementBatchItem> emptyChunk = new Chunk<>();

        // When
        assertThatNoException().isThrownBy(() -> writer.write(emptyChunk));

        // Then
        verify(settlementChunkMapper, never()).updateToCompleted(any());
    }

    @Test
    @DisplayName("유효하지 않은 데이터 포함 - 유효성 검증 실패")
    void write_InvalidData_SkipInvalidItems() throws Exception {
        // Given
        List<SettlementBatchItem> items = List.of(
                createValidCompleteItem("settlement-001", "seller1", "ORDER-001"),
                createInvalidCompleteItem(), // 유효하지 않은 데이터
                createValidCompleteItem("settlement-003", "seller3", "ORDER-003")
        );
        Chunk<SettlementBatchItem> chunk = new Chunk<>(items);

        given(settlementChunkMapper.updateToCompleted("settlement-001")).willReturn(1);
        given(settlementChunkMapper.updateToCompleted("settlement-003")).willReturn(1);

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        // 유효한 2개만 update 호출되어야 함
        verify(settlementChunkMapper, times(2)).updateToCompleted(anyString());
        verify(settlementChunkMapper).updateToCompleted("settlement-001");
        verify(settlementChunkMapper).updateToCompleted("settlement-003");
        verify(settlementChunkMapper, never()).updateToCompleted(isNull());
    }

    @Test
    @DisplayName("업데이트 대상 없음 - updateCount가 0인 경우")
    void write_NoUpdateTarget_HandleGracefully() throws Exception {
        // Given
        List<SettlementBatchItem> items = List.of(
                createValidCompleteItem("settlement-001", "seller1", "ORDER-001"),
                createValidCompleteItem("settlement-002", "seller2", "ORDER-002"), // 이미 완료된 정산
                createValidCompleteItem("settlement-003", "seller3", "ORDER-003")
        );
        Chunk<SettlementBatchItem> chunk = new Chunk<>(items);

        given(settlementChunkMapper.updateToCompleted("settlement-001")).willReturn(1);
        given(settlementChunkMapper.updateToCompleted("settlement-002")).willReturn(0); // 업데이트 안됨
        given(settlementChunkMapper.updateToCompleted("settlement-003")).willReturn(1);

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        verify(settlementChunkMapper, times(3)).updateToCompleted(anyString());
        verify(settlementChunkMapper).updateToCompleted("settlement-001");
        verify(settlementChunkMapper).updateToCompleted("settlement-002");
        verify(settlementChunkMapper).updateToCompleted("settlement-003");
    }

    @Test
    @DisplayName("개별 아이템 업데이트 실패 - 계속 처리")
    void write_IndividualItemFailure_ContinueProcessing() throws Exception {
        // Given
        List<SettlementBatchItem> items = createValidCompleteItems();
        Chunk<SettlementBatchItem> chunk = new Chunk<>(items);

        given(settlementChunkMapper.updateToCompleted("settlement-001")).willReturn(1);
        given(settlementChunkMapper.updateToCompleted("settlement-002"))
                .willThrow(new RuntimeException("DB 업데이트 실패"));
        given(settlementChunkMapper.updateToCompleted("settlement-003")).willReturn(1);

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        // 모든 아이템에 대해 update 시도되어야 함
        verify(settlementChunkMapper, times(3)).updateToCompleted(anyString());
    }

    @Test
    @DisplayName("모든 아이템 업데이트 실패")
    void write_AllItemsFailure() throws Exception {
        // Given
        List<SettlementBatchItem> items = createValidCompleteItems();
        Chunk<SettlementBatchItem> chunk = new Chunk<>(items);

        given(settlementChunkMapper.updateToCompleted(anyString()))
                .willThrow(new RuntimeException("DB 연결 실패"));

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        verify(settlementChunkMapper, times(3)).updateToCompleted(anyString());
    }

    @Test
    @DisplayName("단일 아이템 청크 처리")
    void write_SingleItemChunk_Success() throws Exception {
        // Given
        SettlementBatchItem singleItem = createValidCompleteItem("settlement-123", "seller456", "ORDER-789");
        Chunk<SettlementBatchItem> chunk = new Chunk<>(List.of(singleItem));

        given(settlementChunkMapper.updateToCompleted("settlement-123")).willReturn(1);

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        verify(settlementChunkMapper, times(1)).updateToCompleted("settlement-123");
    }

    @Test
    @DisplayName("대용량 청크 처리")
    void write_LargeChunk_Success() throws Exception {
        // Given
        List<SettlementBatchItem> largeItems = createLargeCompleteItems(50);
        Chunk<SettlementBatchItem> chunk = new Chunk<>(largeItems);

        given(settlementChunkMapper.updateToCompleted(anyString())).willReturn(1);

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        verify(settlementChunkMapper, times(50)).updateToCompleted(anyString());
    }

    @Test
    @DisplayName("정산 완료 처리 필드 검증")
    void write_ValidateCompleteFields() throws Exception {
        // Given
        SettlementBatchItem item = SettlementBatchItem.forComplete(
                "settlement-456",
                "seller789",
                "ORDER-123",
                "프리미엄 간식",
                95000L
        );
        Chunk<SettlementBatchItem> chunk = new Chunk<>(List.of(item));

        given(settlementChunkMapper.updateToCompleted("settlement-456")).willReturn(1);

        // When
        writer.write(chunk);

        // Then
        verify(settlementChunkMapper).updateToCompleted("settlement-456");

        // forComplete로 생성된 아이템의 필드 확인
        assertThat(item.getSettlementId()).isEqualTo("settlement-456");
        assertThat(item.getSellerId()).isEqualTo("seller789");
        assertThat(item.getOrderNumber()).isEqualTo("ORDER-123");
        assertThat(item.getProductTitle()).isEqualTo("프리미엄 간식");
        assertThat(item.getSettlementAmount()).isEqualTo(95000L);

        // 완료 처리에서는 불필요한 필드들이 null이어야 함
        assertThat(item.getOrderItemId()).isNull();
        assertThat(item.getItemPrice()).isNull();
        assertThat(item.getCommissionRate()).isNull();
        assertThat(item.getCommissionAmount()).isNull();
    }

    @Test
    @DisplayName("성공/실패/미처리 카운트 로직 검증")
    void write_CountLogic_MixedResults() throws Exception {
        // Given
        List<SettlementBatchItem> items = List.of(
                createValidCompleteItem("settlement-001", "seller1", "ORDER-001"), // 성공
                createInvalidCompleteItem(), // 유효성 실패
                createValidCompleteItem("settlement-003", "seller3", "ORDER-003"), // 성공
                createValidCompleteItem("settlement-004", "seller4", "ORDER-004"), // 업데이트 없음
                createValidCompleteItem("settlement-005", "seller5", "ORDER-005")  // 예외 발생
        );
        Chunk<SettlementBatchItem> chunk = new Chunk<>(items);

        given(settlementChunkMapper.updateToCompleted("settlement-001")).willReturn(1); // 성공
        given(settlementChunkMapper.updateToCompleted("settlement-003")).willReturn(1); // 성공
        given(settlementChunkMapper.updateToCompleted("settlement-004")).willReturn(0); // 업데이트 없음
        given(settlementChunkMapper.updateToCompleted("settlement-005"))
                .willThrow(new RuntimeException("예외")); // 실패

        // When
        assertThatNoException().isThrownBy(() -> writer.write(chunk));

        // Then
        // 유효한 4개 (무효한 1개 제외)에 대해서만 update 시도
        verify(settlementChunkMapper, times(4)).updateToCompleted(anyString());
    }

    @Test
    @DisplayName("IN_PROGRESS에서 COMPLETED로 상태 변경 검증")
    void write_StatusChange_InProgressToCompleted() throws Exception {
        // Given
        SettlementBatchItem item = createValidCompleteItem("settlement-001", "seller1", "ORDER-001");
        Chunk<SettlementBatchItem> chunk = new Chunk<>(List.of(item));

        given(settlementChunkMapper.updateToCompleted("settlement-001")).willReturn(1);

        // When
        writer.write(chunk);

        // Then
        // updateToCompleted 메서드가 호출되어 IN_PROGRESS → COMPLETED 상태 변경
        verify(settlementChunkMapper).updateToCompleted("settlement-001");
    }

    // === Helper Methods ===

    private List<SettlementBatchItem> createValidCompleteItems() {
        return List.of(
                createValidCompleteItem("settlement-001", "seller1", "ORDER-001"),
                createValidCompleteItem("settlement-002", "seller2", "ORDER-002"),
                createValidCompleteItem("settlement-003", "seller3", "ORDER-003")
        );
    }

    private SettlementBatchItem createValidCompleteItem(String settlementId, String sellerId, String orderNumber) {
        return SettlementBatchItem.forComplete(
                settlementId,
                sellerId,
                orderNumber,
                "상품명_" + sellerId,
                95000L + Long.parseLong(settlementId.substring(settlementId.length() - 1)) * 1000 // 다양한 금액
        );
    }

    private SettlementBatchItem createInvalidCompleteItem() {
        // isValidForComplete()가 false를 반환하도록 하는 무효한 데이터
        return SettlementBatchItem.builder()
                .settlementId(null) // 필수 필드 누락
                .sellerId(null)
                .orderNumber("ORDER-INVALID")
                .productTitle("무효한 상품")
                .settlementAmount(0L)
                .build();
    }

    private List<SettlementBatchItem> createLargeCompleteItems(int count) {
        return java.util.stream.IntStream.range(1, count + 1)
                .mapToObj(i -> createValidCompleteItem(
                        "settlement-" + String.format("%03d", i),
                        "seller" + i,
                        "ORDER-" + String.format("%03d", i)
                ))
                .toList();
    }
}