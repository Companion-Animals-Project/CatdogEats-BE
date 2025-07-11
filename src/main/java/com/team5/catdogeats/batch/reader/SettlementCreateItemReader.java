package com.team5.catdogeats.batch.reader;

import com.team5.catdogeats.batch.dto.SettlementBatchItem;
import com.team5.catdogeats.batch.mapper.SettlementChunkMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;

/**
 * 정산 생성용 ItemReader
 * 동적 데이터 변화에 대응하는 offset 관리 방식 적용
 * Step 스코프로 각 실행마다 새로운 인스턴스 생성
 */
@Slf4j
public class SettlementCreateItemReader implements ItemReader<SettlementBatchItem> {

    private final SettlementChunkMapper settlementChunkMapper;
    private final BigDecimal commissionRate;
    private final int chunkSize;

    private Iterator<SettlementBatchItem> currentChunkIterator;
    private boolean hasMoreData = true;
    private int totalProcessedCount = 0; // 총 처리된 건수 추적

    public SettlementCreateItemReader(SettlementChunkMapper settlementChunkMapper,
                                      BigDecimal commissionRate,
                                      int chunkSize) {
        this.settlementChunkMapper = settlementChunkMapper;
        this.commissionRate = commissionRate;
        this.chunkSize = chunkSize;

        log.info("SettlementCreateItemReader 초기화 - chunkSize: {}, commissionRate: {}",
                chunkSize, commissionRate);
    }

    /**
     * Job/Step 시작 시 상태 초기화
     * StepExecutionListener에서 호출됨
     */
    public void reset() {
        log.info("🔄 [RESET] 배치 시작 - hasMoreData 리셋: {} → true", hasMoreData);
        this.hasMoreData = true;
        this.totalProcessedCount = 0;
        this.currentChunkIterator = null;
        log.info("🔄 [RESET] 완료 - totalProcessedCount: 0, currentChunkIterator: null");
    }

    @Override
    public SettlementBatchItem read() throws UnexpectedInputException,
            ParseException, NonTransientResourceException {

        // 현재 청크에서 더 읽을 데이터가 있는지 확인
        if (currentChunkIterator == null || !currentChunkIterator.hasNext()) {
            // 더 이상 읽을 데이터가 없으면 null 반환
            if (!hasMoreData) {
                log.info("정산 생성 대상 데이터 읽기 완료 - 총 처리된 건수: {}", totalProcessedCount);
                return null;
            }

            // 다음 청크 데이터 로드
            loadNextChunk();

            // 새로 로드한 청크에도 데이터가 없으면 null 반환
            if (currentChunkIterator == null || !currentChunkIterator.hasNext()) {
                log.info("정산 생성 대상 데이터가 더 이상 없습니다 - 총 처리된 건수: {}", totalProcessedCount);
                return null;
            }
        }

        // 다음 아이템 반환
        SettlementBatchItem item = currentChunkIterator.next();
        totalProcessedCount++; // 처리된 건수 증가

        // 수수료 정보 설정 (Reader에서 설정하여 Writer는 단순 저장만 하도록)
        SettlementBatchItem enrichedItem = SettlementBatchItem.forCreate(
                item.getSellerId(),
                item.getOrderItemId(),
                item.getOrderNumber(),
                item.getProductTitle(),
                item.getItemPrice(),
                commissionRate
        );

        log.debug("정산 생성 아이템 읽기 - orderNumber: {}, itemPrice: {}, settlementAmount: {}, 총처리건수: {}",
                enrichedItem.getOrderNumber(), enrichedItem.getItemPrice(), enrichedItem.getSettlementAmount(), totalProcessedCount);

        return enrichedItem;
    }

    /**
     * 다음 청크 데이터 로드 - 고정 offset 0 사용
     * 이미 처리된 데이터는 쿼리 조건(LEFT JOIN settlements WHERE st.id IS NULL)에서 자동 제외됨
     */
    private void loadNextChunk() {
        try {
            log.debug("정산 생성 대상 데이터 로드 시작 - offset: 0 (고정), limit: {}", chunkSize);

            // 항상 offset 0부터 조회 - 쿼리에서 이미 처리된 데이터는 자동 제외됨
            List<SettlementBatchItem> chunk = settlementChunkMapper.findUnsettledItems(0, chunkSize);

            if (chunk.isEmpty()) {
                log.info("정산 생성 대상 데이터 없음 - 모든 데이터 처리 완료");
                hasMoreData = false;
                currentChunkIterator = null;
                return;
            }

            log.info("정산 생성 대상 데이터 로드 완료 - 현재 청크 건수: {}, 총 처리 예정: {}",
                    chunk.size(), totalProcessedCount + chunk.size());

            currentChunkIterator = chunk.iterator();

            // 로드된 데이터가 청크 사이즈보다 적으면 더 이상 데이터가 없음을 의미
            if (chunk.size() < chunkSize) {
                log.info("마지막 청크 감지 - 로드된 건수: {} < 청크 사이즈: {}", chunk.size(), chunkSize);
                hasMoreData = false;
            }

        } catch (Exception e) {
            log.error("정산 생성 대상 데이터 로드 실패", e);
            throw new RuntimeException("정산 생성 ItemReader에서 데이터 로드 실패", e);
        }
    }
}