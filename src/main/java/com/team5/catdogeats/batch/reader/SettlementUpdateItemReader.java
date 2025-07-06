package com.team5.catdogeats.batch.reader;

import com.team5.catdogeats.orders.domain.dto.SettlementBatchItem;
import com.team5.catdogeats.orders.mapper.SettlementChunkMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.util.Iterator;
import java.util.List;

/**
 * 수정된 정산 상태 갱신용 ItemReader
 * 동적 데이터 변화에 대응하는 offset 관리 방식 적용
 */
@Slf4j
public class SettlementUpdateItemReader implements ItemReader<SettlementBatchItem> {

    private final SettlementChunkMapper settlementChunkMapper;
    private final int chunkSize;

    private Iterator<SettlementBatchItem> currentChunkIterator;
    private boolean hasMoreData = true;
    private int totalProcessedCount = 0;

    public SettlementUpdateItemReader(SettlementChunkMapper settlementChunkMapper, int chunkSize) {
        this.settlementChunkMapper = settlementChunkMapper;
        this.chunkSize = chunkSize;

        log.info("SettlementUpdateItemReader 초기화 - chunkSize: {}", chunkSize);
    }

    @Override
    public SettlementBatchItem read() throws Exception, UnexpectedInputException,
            ParseException, NonTransientResourceException {

        // 현재 청크에서 더 읽을 데이터가 있는지 확인
        if (currentChunkIterator == null || !currentChunkIterator.hasNext()) {
            // 더 이상 읽을 데이터가 없으면 null 반환
            if (!hasMoreData) {
                log.info("정산 상태 갱신 대상 데이터 읽기 완료 - 총 처리된 건수: {}", totalProcessedCount);
                return null;
            }

            // 다음 청크 데이터 로드
            loadNextChunk();

            // 새로 로드한 청크에도 데이터가 없으면 null 반환
            if (currentChunkIterator == null || !currentChunkIterator.hasNext()) {
                log.info("정산 상태 갱신 대상 데이터가 더 이상 없습니다 - 총 처리된 건수: {}", totalProcessedCount);
                return null;
            }
        }

        // 다음 아이템 반환
        SettlementBatchItem item = currentChunkIterator.next();
        totalProcessedCount++;

        // 상태 갱신용 아이템으로 변환
        SettlementBatchItem updateItem = SettlementBatchItem.forUpdate(
                item.getSettlementId(),
                item.getSellerId(),
                item.getOrderNumber(),
                item.getProductTitle(),
                item.getDeliveredAt()
        );

        log.debug("정산 상태 갱신 아이템 읽기 - settlementId: {}, orderNumber: {}, 총처리건수: {}",
                updateItem.getSettlementId(), updateItem.getOrderNumber(), totalProcessedCount);

        return updateItem;
    }

    /**
     * 다음 청크 데이터 로드 - 고정 offset 0 사용
     * 이미 처리된 데이터는 쿼리 조건(settlement_status = 'PENDING')에서 자동 제외됨
     */
    private void loadNextChunk() {
        try {
            log.debug("정산 상태 갱신 대상 데이터 로드 시작 - offset: 0 (고정), limit: {}", chunkSize);

            // 항상 offset 0부터 조회 - PENDING 상태인 것만 조회되고 처리되면 IN_PROGRESS로 변경되어 제외됨
            List<SettlementBatchItem> chunk = settlementChunkMapper
                    .findPendingSettlementsReadyForProgress(0, chunkSize);

            if (chunk.isEmpty()) {
                log.info("정산 상태 갱신 대상 데이터 없음 - 모든 PENDING 데이터 처리 완료");
                hasMoreData = false;
                currentChunkIterator = null;
                return;
            }

            log.info("정산 상태 갱신 대상 데이터 로드 완료 - 현재 청크 건수: {}, 총 처리 예정: {}",
                    chunk.size(), totalProcessedCount + chunk.size());

            currentChunkIterator = chunk.iterator();

            // 로드된 데이터가 청크 사이즈보다 적으면 더 이상 데이터가 없음을 의미
            if (chunk.size() < chunkSize) {
                log.info("마지막 청크 감지 - 로드된 건수: {} < 청크 사이즈: {}", chunk.size(), chunkSize);
                hasMoreData = false;
            }

        } catch (Exception e) {
            log.error("정산 상태 갱신 대상 데이터 로드 실패", e);
            throw new RuntimeException("정산 상태 갱신 ItemReader에서 데이터 로드 실패", e);
        }
    }

    /**
     * Reader 초기화 (재실행 시 상태 초기화)
     */
    public void reset() {
        log.info("SettlementUpdateItemReader 상태 초기화");
        hasMoreData = true;
        currentChunkIterator = null;
        totalProcessedCount = 0;
    }

    /**
     * 현재 처리된 건수 조회 (모니터링용)
     */
    public int getTotalProcessedCount() {
        return totalProcessedCount;
    }

    /**
     * 더 읽을 데이터가 있는지 확인
     */
    public boolean hasMoreData() {
        return hasMoreData;
    }
}