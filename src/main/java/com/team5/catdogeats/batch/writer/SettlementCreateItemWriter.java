package com.team5.catdogeats.batch.writer;

import com.team5.catdogeats.orders.domain.dto.SettlementBatchItem;
import com.team5.catdogeats.orders.mapper.SettlementChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * 정산 생성용 ItemWriter
 * 청크 단위로 정산 데이터를 DB에 저장
 */
@Slf4j
@RequiredArgsConstructor
public class SettlementCreateItemWriter implements ItemWriter<SettlementBatchItem> {

    private final SettlementChunkMapper settlementChunkMapper;

    @Override
    public void write(Chunk<? extends SettlementBatchItem> chunk) throws Exception {
        if (chunk.isEmpty()) {
            log.debug("정산 생성 Writer - 처리할 데이터 없음");
            return;
        }

        log.info("정산 생성 Writer 시작 - 처리 대상 건수: {}", chunk.size());

        int successCount = 0;
        int failCount = 0;

        for (SettlementBatchItem item : chunk) {
            try {
                // 데이터 유효성 검증
                if (!item.isValidForCreate()) {
                    log.warn("정산 생성 데이터 유효성 검증 실패 - item: {}", item);
                    failCount++;
                    continue;
                }

                // 정산 데이터 생성
                settlementChunkMapper.insertSettlement(item);
                successCount++;

                log.debug("정산 생성 완료 - orderNumber: {}, sellerId: {}, settlementAmount: {}",
                        item.getOrderNumber(), item.getSellerId(), item.getSettlementAmount());

            } catch (Exception e) {
                log.error("정산 생성 실패 - item: {}", item, e);
                failCount++;

                // 개별 아이템 실패는 로그만 남기고 계속 처리
                // (Spring Batch의 Skip 정책으로 별도 제어)
            }
        }

        log.info("정산 생성 Writer 완료 - 성공: {}, 실패: {}, 총 처리: {}",
                successCount, failCount, chunk.size());

        // 실패가 있으면 경고 로그
        if (failCount > 0) {
            log.warn("정산 생성 중 일부 실패 발생 - 실패 건수: {}/{}", failCount, chunk.size());
        }
    }
}