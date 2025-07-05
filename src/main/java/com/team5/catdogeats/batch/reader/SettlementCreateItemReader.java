package com.team5.catdogeats.batch.reader;

import com.team5.catdogeats.orders.domain.dto.SettlementBatchItem;
import com.team5.catdogeats.orders.mapper.SettlementChunkMapper;
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
 * 배송완료되었지만 정산이 생성되지 않은 주문아이템들을 청크 단위로 읽어옴
 */
@Slf4j
public class SettlementCreateItemReader implements ItemReader<SettlementBatchItem> {

    private final SettlementChunkMapper settlementChunkMapper;
    private final BigDecimal commissionRate;
    private final int chunkSize;

    private Iterator<SettlementBatchItem> currentChunkIterator;
    private int currentOffset = 0;
    private boolean hasMoreData = true;

    public SettlementCreateItemReader(SettlementChunkMapper settlementChunkMapper,
                                      BigDecimal commissionRate,
                                      int chunkSize) {
        this.settlementChunkMapper = settlementChunkMapper;
        this.commissionRate = commissionRate;
        this.chunkSize = chunkSize;

        log.info("SettlementCreateItemReader 초기화 - chunkSize: {}, commissionRate: {}",
                chunkSize, commissionRate);
    }

    @Override
    public SettlementBatchItem read() throws Exception, UnexpectedInputException,
            ParseException, NonTransientResourceException {

        // 현재 청크에서 더 읽을 데이터가 있는지 확인
        if (currentChunkIterator == null || !currentChunkIterator.hasNext()) {
            // 더 이상 읽을 데이터가 없으면 null 반환
            if (!hasMoreData) {
                log.info("정산 생성 대상 데이터 읽기 완료 - 총 처리된 offset: {}", currentOffset);
                return null;
            }

            // 다음 청크 데이터 로드
            loadNextChunk();

            // 새로 로드한 청크에도 데이터가 없으면 null 반환
            if (currentChunkIterator == null || !currentChunkIterator.hasNext()) {
                log.info("정산 생성 대상 데이터가 더 이상 없습니다 - offset: {}", currentOffset);
                return null;
            }
        }

        // 다음 아이템 반환
        SettlementBatchItem item = currentChunkIterator.next();

        // 수수료 정보 설정 (Reader에서 설정하여 Writer는 단순 저장만 하도록)
        SettlementBatchItem enrichedItem = SettlementBatchItem.forCreate(
                item.getSellerId(),
                item.getOrderItemId(),
                item.getOrderNumber(),
                item.getProductTitle(),
                item.getItemPrice(),
                commissionRate
        );

        log.debug("정산 생성 아이템 읽기 - orderNumber: {}, itemPrice: {}, settlementAmount: {}",
                enrichedItem.getOrderNumber(), enrichedItem.getItemPrice(), enrichedItem.getSettlementAmount());

        return enrichedItem;
    }

    /**
     * 다음 청크 데이터 로드
     */
    private void loadNextChunk() {
        try {
            log.debug("정산 생성 대상 데이터 로드 시작 - offset: {}, limit: {}", currentOffset, chunkSize);

            List<SettlementBatchItem> chunk = settlementChunkMapper.findUnsettledItems(currentOffset, chunkSize);

            if (chunk.isEmpty()) {
                log.info("정산 생성 대상 데이터 없음 - offset: {}", currentOffset);
                hasMoreData = false;
                currentChunkIterator = null;
                return;
            }

            log.info("정산 생성 대상 데이터 로드 완료 - offset: {}, 로드된 건수: {}", currentOffset, chunk.size());

            currentChunkIterator = chunk.iterator();
            currentOffset += chunk.size();

            // 로드된 데이터가 청크 사이즈보다 적으면 더 이상 데이터가 없음을 의미
            if (chunk.size() < chunkSize) {
                hasMoreData = false;
            }

        } catch (Exception e) {
            log.error("정산 생성 대상 데이터 로드 실패 - offset: {}", currentOffset, e);
            throw new RuntimeException("정산 생성 ItemReader에서 데이터 로드 실패", e);
        }
    }

    /**
     * Reader 초기화 (재실행 시 상태 초기화)
     */
    public void reset() {
        log.info("SettlementCreateItemReader 상태 초기화");
        currentOffset = 0;
        hasMoreData = true;
        currentChunkIterator = null;
    }

    /**
     * 현재 진행 상황 조회 (모니터링용)
     */
    public int getCurrentOffset() {
        return currentOffset;
    }

    /**
     * 더 읽을 데이터가 있는지 확인
     */
    public boolean hasMoreData() {
        return hasMoreData;
    }
}