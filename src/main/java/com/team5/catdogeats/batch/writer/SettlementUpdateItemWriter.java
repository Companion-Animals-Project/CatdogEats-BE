package com.team5.catdogeats.batch.writer;

import com.team5.catdogeats.batch.dto.SettlementBatchItem;
import com.team5.catdogeats.batch.mapper.SettlementChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * 정산 상태 갱신용 ItemWriter
 * PENDING 상태의 정산을 IN_PROGRESS로 변경
 */
@Slf4j
@RequiredArgsConstructor
public class SettlementUpdateItemWriter implements ItemWriter<SettlementBatchItem> {

    private final SettlementChunkMapper settlementChunkMapper;

    @Override
    public void write(Chunk<? extends SettlementBatchItem> chunk) throws Exception {
        if (chunk.isEmpty()) {
            log.debug("정산 상태 갱신 Writer - 처리할 데이터 없음");
            return;
        }

        log.info("정산 상태 갱신 Writer 시작 - 처리 대상 건수: {}", chunk.size());

        int successCount = 0;
        int failCount = 0;
        int noUpdateCount = 0;

        for (SettlementBatchItem item : chunk) {
            try {
                // 데이터 유효성 검증
                if (!item.isValidForUpdate()) {
                    log.warn("정산 상태 갱신 데이터 유효성 검증 실패 - item: {}", item);
                    failCount++;
                    continue;
                }

                // 정산 상태를 IN_PROGRESS로 변경
                int updateCount = settlementChunkMapper.updateToInProgress(item.getSettlementId());

                if (updateCount == 1) {
                    successCount++;
                    log.debug("정산 상태 갱신 완료 - settlementId: {}, orderNumber: {}",
                            item.getSettlementId(), item.getOrderNumber());
                } else {
                    // 업데이트된 행이 없는 경우 (이미 상태가 변경되었거나 데이터가 없음)
                    noUpdateCount++;
                    log.warn("정산 상태 갱신 실패 - 대상 데이터 없음. settlementId: {}, orderNumber: {}",
                            item.getSettlementId(), item.getOrderNumber());
                }

            } catch (Exception e) {
                log.error("정산 상태 갱신 실패 - item: {}", item, e);
                failCount++;

                // 개별 아이템 실패는 로그만 남기고 계속 처리
                // (Spring Batch의 Skip 정책으로 별도 제어)
            }
        }

        log.info("정산 상태 갱신 Writer 완료 - 성공: {}, 업데이트없음: {}, 실패: {}, 총 처리: {}",
                successCount, noUpdateCount, failCount, chunk.size());

        // 실패나 업데이트되지 않은 건이 있으면 경고 로그
        if (failCount > 0 || noUpdateCount > 0) {
            log.warn("정산 상태 갱신 중 일부 실패/미처리 발생 - 실패: {}, 미처리: {}, 총: {}",
                    failCount, noUpdateCount, chunk.size());
        }
    }
}