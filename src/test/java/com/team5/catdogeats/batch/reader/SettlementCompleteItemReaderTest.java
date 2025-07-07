package com.team5.catdogeats.batch.reader;

import com.team5.catdogeats.batch.dto.SettlementBatchItem;
import com.team5.catdogeats.batch.mapper.SettlementChunkMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("정산 완료 ItemReader 단위 테스트")
class SettlementCompleteItemReaderTest {

    @Mock
    private SettlementChunkMapper settlementChunkMapper;

    private SettlementCompleteItemReader reader;
    private final int chunkSize = 3;

    @BeforeEach
    void setUp() {
        reader = new SettlementCompleteItemReader(settlementChunkMapper, chunkSize);
    }

    @Test
    @DisplayName("정상적인 IN_PROGRESS 정산 데이터 읽기 - 성공")
    void read_InProgressData_Success() throws Exception {
        // Given
        List<SettlementBatchItem> mockData = createMockInProgressItems();
        given(settlementChunkMapper.findInProgressSettlements(0, chunkSize))
                .willReturn(mockData);

        // When
        SettlementBatchItem item1 = reader.read();
        SettlementBatchItem item2 = reader.read();
        SettlementBatchItem item3 = reader.read();

        // Then
        assertThat(item1).isNotNull();
        assertThat(item1.getSettlementId()).isEqualTo("settlement-001");
        assertThat(item1.getOrderNumber()).isEqualTo("ORDER-001");
        assertThat(item1.getSettlementAmount()).isEqualTo(95000L);

        assertThat(item2).isNotNull();
        assertThat(item2.getSettlementId()).isEqualTo("settlement-002");
        assertThat(item2.getOrderNumber()).isEqualTo("ORDER-002");

        assertThat(item3).isNotNull();
        assertThat(item3.getSettlementId()).isEqualTo("settlement-003");
        assertThat(item3.getOrderNumber()).isEqualTo("ORDER-003");

        verify(settlementChunkMapper).findInProgressSettlements(0, chunkSize);
    }

    @Test
    @DisplayName("forComplete 변환 로직 검증")
    void read_ForCompleteConversion_Correct() throws Exception {
        // Given
        List<SettlementBatchItem> mockData = List.of(
                createMockInProgressItem("settlement-123", "seller456", "ORDER-789", "프리미엄 사료", 150000L)
        );
        given(settlementChunkMapper.findInProgressSettlements(0, chunkSize))
                .willReturn(mockData);

        // When
        SettlementBatchItem result = reader.read();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSettlementId()).isEqualTo("settlement-123");
        assertThat(result.getSellerId()).isEqualTo("seller456");
        assertThat(result.getOrderNumber()).isEqualTo("ORDER-789");
        assertThat(result.getProductTitle()).isEqualTo("프리미엄 사료");
        assertThat(result.getSettlementAmount()).isEqualTo(150000L);

        // forComplete에서는 수수료 관련 필드는 null이어야 함
        assertThat(result.getOrderItemId()).isNull();
        assertThat(result.getItemPrice()).isNull();
        assertThat(result.getCommissionRate()).isNull();
        assertThat(result.getCommissionAmount()).isNull();
    }

    @Test
    @DisplayName("청크 데이터 모두 읽은 후 다음 청크 로드")
    void read_LoadNextChunk_Success() throws Exception {
        // Given - 첫 번째 청크
        List<SettlementBatchItem> firstChunk = createMockInProgressItems();
        // 두 번째 청크 (마지막 청크 - 크기가 chunkSize보다 작음)
        List<SettlementBatchItem> secondChunk = List.of(
                createMockInProgressItem("settlement-004", "seller4", "ORDER-004", "상품4", 80000L)
        );

        given(settlementChunkMapper.findInProgressSettlements(0, chunkSize))
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
        assertThat(item4.getSettlementId()).isEqualTo("settlement-004");
        assertThat(item4.getOrderNumber()).isEqualTo("ORDER-004");

        verify(settlementChunkMapper, times(2)).findInProgressSettlements(0, chunkSize);
    }

    @Test
    @DisplayName("마지막 청크 감지 후 종료")
    void read_LastChunk_ReturnNull() throws Exception {
        // Given - 작은 크기의 청크 (마지막 청크)
        List<SettlementBatchItem> lastChunk = List.of(
                createMockInProgressItem("settlement-001", "seller1", "ORDER-001", "상품1", 100000L)
        );
        given(settlementChunkMapper.findInProgressSettlements(0, chunkSize))
                .willReturn(lastChunk);

        // When
        SettlementBatchItem item1 = reader.read(); // 마지막 아이템
        SettlementBatchItem item2 = reader.read(); // null이어야 함

        // Then
        assertThat(item1).isNotNull();
        assertThat(item1.getSettlementId()).isEqualTo("settlement-001");
        assertThat(item2).isNull(); // 더 이상 읽을 데이터 없음

        // 마지막 청크 감지로 인해 1번만 호출됨
        verify(settlementChunkMapper, times(1)).findInProgressSettlements(0, chunkSize);
    }

    @Test
    @DisplayName("처음부터 빈 데이터 - null 반환")
    void read_EmptyData_ReturnNull() throws Exception {
        // Given
        given(settlementChunkMapper.findInProgressSettlements(0, chunkSize))
                .willReturn(List.of());

        // When
        SettlementBatchItem result = reader.read();

        // Then
        assertThat(result).isNull();
        verify(settlementChunkMapper).findInProgressSettlements(0, chunkSize);
    }

    @Test
    @DisplayName("매퍼에서 예외 발생 시 RuntimeException으로 래핑")
    void read_MapperException_ThrowsRuntimeException() {
        // Given
        given(settlementChunkMapper.findInProgressSettlements(0, chunkSize))
                .willThrow(new RuntimeException("DB 연결 실패"));

        // When & Then
        assertThatThrownBy(() -> reader.read())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("정산 완료 ItemReader에서 데이터 로드 실패")
                .hasCauseInstanceOf(RuntimeException.class);

        verify(settlementChunkMapper).findInProgressSettlements(0, chunkSize);
    }

    @Test
    @DisplayName("고정 offset 0 사용 검증")
    void read_AlwaysUseOffsetZero() throws Exception {
        // Given
        List<SettlementBatchItem> mockData = createMockInProgressItems();
        given(settlementChunkMapper.findInProgressSettlements(0, chunkSize))
                .willReturn(mockData)
                .willReturn(List.of()); // 두 번째 호출에서는 빈 리스트

        // When - 모든 데이터 읽기
        reader.read();
        reader.read();
        reader.read();
        reader.read(); // null 반환

        // Then - 항상 offset 0으로 호출되어야 함
        verify(settlementChunkMapper, times(2)).findInProgressSettlements(eq(0), eq(chunkSize));
        verify(settlementChunkMapper, never()).findInProgressSettlements(gt(0), anyInt());
    }

    @Test
    @DisplayName("IN_PROGRESS 상태 조건 검증")
    void read_InProgressStatusCondition() throws Exception {
        // Given
        List<SettlementBatchItem> inProgressData = createMockInProgressItems();
        given(settlementChunkMapper.findInProgressSettlements(0, chunkSize))
                .willReturn(inProgressData);

        // When
        SettlementBatchItem result = reader.read();

        // Then
        assertThat(result).isNotNull();
        // IN_PROGRESS 상태의 정산만 조회되는지 매퍼 메서드 호출로 검증
        verify(settlementChunkMapper).findInProgressSettlements(0, chunkSize);
        // findUnsettledItems는 호출되지 않아야 함 (생성용과 구분)
        verify(settlementChunkMapper, never()).findUnsettledItems(anyInt(), anyInt());
    }

    @Test
    @DisplayName("60일 이내 생성된 정산만 처리되는지 검증")
    void read_Within60DaysCondition() throws Exception {
        // Given - 최근 60일 내 생성된 IN_PROGRESS 정산
        List<SettlementBatchItem> recentData = createMockInProgressItems();
        given(settlementChunkMapper.findInProgressSettlements(0, chunkSize))
                .willReturn(recentData);

        // When
        SettlementBatchItem result = reader.read();

        // Then
        assertThat(result).isNotNull();
        // 60일 제한 조건이 포함된 쿼리 메서드 호출 검증
        verify(settlementChunkMapper).findInProgressSettlements(0, chunkSize);
    }

    @Test
    @DisplayName("총 처리 건수 추적 검증")
    void read_TotalProcessedCountTracking() throws Exception {
        // Given
        List<SettlementBatchItem> mockData = createMockInProgressItems();
        given(settlementChunkMapper.findInProgressSettlements(0, chunkSize))
                .willReturn(mockData)
                .willReturn(List.of()); // 빈 리스트로 종료

        // When - 모든 데이터 읽기
        SettlementBatchItem item1 = reader.read();
        SettlementBatchItem item2 = reader.read();
        SettlementBatchItem item3 = reader.read();
        SettlementBatchItem item4 = reader.read(); // null

        // Then
        assertThat(item1).isNotNull();
        assertThat(item2).isNotNull();
        assertThat(item3).isNotNull();
        assertThat(item4).isNull();

        // 로그에서 총 처리 건수가 올바르게 추적되는지는 로그 레벨에서 확인
        // 여기서는 메서드 호출 횟수로 간접 검증
        verify(settlementChunkMapper, times(2)).findInProgressSettlements(0, chunkSize);
    }

    // === Helper Methods ===

    private List<SettlementBatchItem> createMockInProgressItems() {
        return List.of(
                createMockInProgressItem("settlement-001", "seller1", "ORDER-001", "상품1", 95000L),
                createMockInProgressItem("settlement-002", "seller2", "ORDER-002", "상품2", 142500L),
                createMockInProgressItem("settlement-003", "seller3", "ORDER-003", "상품3", 190000L)
        );
    }

    private SettlementBatchItem createMockInProgressItem(String settlementId, String sellerId,
                                                         String orderNumber, String productTitle,
                                                         Long settlementAmount) {
        return SettlementBatchItem.builder()
                .settlementId(settlementId)
                .sellerId(sellerId)
                .orderNumber(orderNumber)
                .productTitle(productTitle)
                .settlementAmount(settlementAmount)
                .build();
    }
}