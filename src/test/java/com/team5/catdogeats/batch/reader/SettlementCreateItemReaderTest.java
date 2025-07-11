package com.team5.catdogeats.batch.reader;

import com.team5.catdogeats.batch.dto.SettlementBatchItem;
import com.team5.catdogeats.batch.mapper.SettlementChunkMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("정산 생성 ItemReader 단위 테스트")
class SettlementCreateItemReaderTest {

    @Mock
    private SettlementChunkMapper settlementChunkMapper;

    private SettlementCreateItemReader reader;
    private final BigDecimal commissionRate = new BigDecimal("0.05"); // 5%
    private final int chunkSize = 3;

    @BeforeEach
    void setUp() {
        reader = new SettlementCreateItemReader(settlementChunkMapper, commissionRate, chunkSize);
    }

    @Test
    @DisplayName("정상적인 데이터 읽기 - 성공")
    void read_NormalData_Success() throws Exception {
        // Given
        List<SettlementBatchItem> mockData = createMockUnsettledItems();
        given(settlementChunkMapper.findUnsettledItems(0, chunkSize))
                .willReturn(mockData);

        // When
        SettlementBatchItem item1 = reader.read();
        SettlementBatchItem item2 = reader.read();
        SettlementBatchItem item3 = reader.read();

        // Then
        assertThat(item1).isNotNull();
        assertThat(item1.getOrderNumber()).isEqualTo("ORDER-001");
        assertThat(item1.getItemPrice()).isEqualTo(100000L);
        assertThat(item1.getCommissionRate()).isEqualTo(commissionRate);
        assertThat(item1.getCommissionAmount()).isEqualTo(5000L); // 100000 * 0.05
        assertThat(item1.getSettlementAmount()).isEqualTo(95000L); // 100000 - 5000

        assertThat(item2).isNotNull();
        assertThat(item2.getOrderNumber()).isEqualTo("ORDER-002");

        assertThat(item3).isNotNull();
        assertThat(item3.getOrderNumber()).isEqualTo("ORDER-003");

        verify(settlementChunkMapper).findUnsettledItems(0, chunkSize);
    }

    @Test
    @DisplayName("수수료 계산 로직 검증")
    void read_CommissionCalculation_Correct() throws Exception {
        // Given
        List<SettlementBatchItem> mockData = List.of(
                createMockItem("seller1", "item1", "ORDER-001", "상품1", 200000L)
        );
        given(settlementChunkMapper.findUnsettledItems(0, chunkSize))
                .willReturn(mockData);

        // When
        SettlementBatchItem result = reader.read();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItemPrice()).isEqualTo(200000L);
        assertThat(result.getCommissionRate()).isEqualTo(new BigDecimal("0.05"));
        assertThat(result.getCommissionAmount()).isEqualTo(10000L); // 200000 * 0.05
        assertThat(result.getSettlementAmount()).isEqualTo(190000L); // 200000 - 10000
    }

    @Test
    @DisplayName("청크 데이터 모두 읽은 후 다음 청크 로드")
    void read_LoadNextChunk_Success() throws Exception {
        // Given - 첫 번째 청크
        List<SettlementBatchItem> firstChunk = createMockUnsettledItems();
        // 두 번째 청크 (마지막 청크 - 크기가 chunkSize보다 작음)
        List<SettlementBatchItem> secondChunk = List.of(
                createMockItem("seller4", "item4", "ORDER-004", "상품4", 80000L)
        );

        given(settlementChunkMapper.findUnsettledItems(0, chunkSize))
                .willReturn(firstChunk)
                .willReturn(secondChunk);

        // When - 첫 번째 청크 모두 읽기
        reader.read(); // item1
        reader.read(); // item2
        reader.read(); // item3

        // 다음 청크 로드 후 읽기
        SettlementBatchItem item4 = reader.read();

        // Then
        assertThat(item4).isNotNull();
        assertThat(item4.getOrderNumber()).isEqualTo("ORDER-004");

        verify(settlementChunkMapper, times(2)).findUnsettledItems(0, chunkSize);
    }

    @Test
    @DisplayName("마지막 청크 감지 후 종료")
    void read_LastChunk_ReturnNull() throws Exception {
        // Given - 작은 크기의 청크 (마지막 청크)
        List<SettlementBatchItem> lastChunk = List.of(
                createMockItem("seller1", "item1", "ORDER-001", "상품1", 100000L)
        );
        given(settlementChunkMapper.findUnsettledItems(0, chunkSize))
                .willReturn(lastChunk);

        // When
        SettlementBatchItem item1 = reader.read(); // 마지막 아이템
        SettlementBatchItem item2 = reader.read(); // null이어야 함

        // Then
        assertThat(item1).isNotNull();
        assertThat(item1.getOrderNumber()).isEqualTo("ORDER-001");
        assertThat(item2).isNull(); // 더 이상 읽을 데이터 없음

        // 마지막 청크 감지로 인해 1번만 호출됨
        verify(settlementChunkMapper, times(1)).findUnsettledItems(0, chunkSize);
    }

    @Test
    @DisplayName("처음부터 빈 데이터 - null 반환")
    void read_EmptyData_ReturnNull() throws Exception {
        // Given
        given(settlementChunkMapper.findUnsettledItems(0, chunkSize))
                .willReturn(List.of());

        // When
        SettlementBatchItem result = reader.read();

        // Then
        assertThat(result).isNull();
        verify(settlementChunkMapper).findUnsettledItems(0, chunkSize);
    }

    @Test
    @DisplayName("매퍼에서 예외 발생 시 RuntimeException으로 래핑")
    void read_MapperException_ThrowsRuntimeException() {
        // Given
        given(settlementChunkMapper.findUnsettledItems(0, chunkSize))
                .willThrow(new RuntimeException("DB 연결 실패"));

        // When & Then
        assertThatThrownBy(() -> reader.read())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("정산 생성 ItemReader에서 데이터 로드 실패")
                .hasCauseInstanceOf(RuntimeException.class);

        verify(settlementChunkMapper).findUnsettledItems(0, chunkSize);
    }

    @Test
    @DisplayName("고정 offset 0 사용 검증")
    void read_AlwaysUseOffsetZero() throws Exception {
        // Given
        List<SettlementBatchItem> mockData = createMockUnsettledItems();
        given(settlementChunkMapper.findUnsettledItems(0, chunkSize))
                .willReturn(mockData)
                .willReturn(List.of()); // 두 번째 호출에서는 빈 리스트

        // When - 모든 데이터 읽기
        reader.read();
        reader.read();
        reader.read();
        reader.read(); // null 반환

        // Then - 항상 offset 0으로 호출되어야 함
        verify(settlementChunkMapper, times(2)).findUnsettledItems(eq(0), eq(chunkSize));
        verify(settlementChunkMapper, never()).findUnsettledItems(gt(0), anyInt());
    }

    // === Helper Methods ===

    private List<SettlementBatchItem> createMockUnsettledItems() {
        return List.of(
                createMockItem("seller1", "item1", "ORDER-001", "상품1", 100000L),
                createMockItem("seller2", "item2", "ORDER-002", "상품2", 150000L),
                createMockItem("seller3", "item3", "ORDER-003", "상품3", 200000L)
        );
    }

    private SettlementBatchItem createMockItem(String sellerId, String orderItemId,
                                               String orderNumber, String productTitle, Long itemPrice) {
        return SettlementBatchItem.builder()
                .sellerId(sellerId)
                .orderItemId(orderItemId)
                .orderNumber(orderNumber)
                .productTitle(productTitle)
                .itemPrice(itemPrice)
                .build();
    }
}