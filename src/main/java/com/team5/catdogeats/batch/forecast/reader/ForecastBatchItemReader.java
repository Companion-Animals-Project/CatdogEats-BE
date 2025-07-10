package com.team5.catdogeats.batch.forecast.reader;

import com.team5.catdogeats.batch.forecast.dto.ForecastBatchItem;
import com.team5.catdogeats.batch.mapper.ForecastBatchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

/**
 * 수요예측 배치 ItemReader
 * 판매자를 청크 단위로 읽어와서 처리
 */
@Slf4j
@RequiredArgsConstructor
public class ForecastBatchItemReader implements ItemReader<ForecastBatchItem> {

    private final ForecastBatchMapper forecastBatchMapper;
    private final int chunkSize;

    // Reader 상태 관리
    private Iterator<ForecastBatchItem> currentIterator;
    private int currentOffset = 0;
    private boolean hasMoreData = true;
    private long totalItemCount = 0;
    private long processedItemCount = 0;

    /**
     * 다음 아이템 읽기
     */
    @Override
    public ForecastBatchItem read() throws Exception {
        // 첫 번째 호출시 초기화
        if (currentIterator == null) {
            initializeReader();
        }

        // 현재 청크에 더 이상 데이터가 없으면 다음 청크 로드
        if (!currentIterator.hasNext() && hasMoreData) {
            loadNextChunk();
        }

        // 읽을 데이터가 있으면 반환
        if (currentIterator.hasNext()) {
            ForecastBatchItem item = currentIterator.next();
            processedItemCount++;

            log.debug("ItemReader - 판매자 읽기: {} ({}/{}) - {}",
                    item.getSellerId(), processedItemCount, totalItemCount, item.getVendorName());

            return item;
        }

        // 더 이상 읽을 데이터가 없음
        log.info("ItemReader 완료 - 총 처리 대상: {}개", processedItemCount);
        return null;
    }

    /**
     * Reader 초기화
     */
    private void initializeReader() {
        log.info("수요예측 배치 ItemReader 초기화 시작");

        try {
            // 전체 처리 대상 개수 조회
            totalItemCount = forecastBatchMapper.countSellersForForecastBatch();
            log.info("총 처리 대상 판매자 수: {}개", totalItemCount);

            if (totalItemCount == 0) {
                log.warn("처리할 판매자가 없습니다");
                hasMoreData = false;
                currentIterator = List.<ForecastBatchItem>of().iterator();
                return;
            }

            // 첫 번째 청크 로드
            loadNextChunk();

        } catch (Exception e) {
            log.error("ItemReader 초기화 실패", e);
            throw new RuntimeException("ItemReader 초기화 중 오류 발생", e);
        }
    }

    /**
     * 다음 청크 데이터 로드
     */
    private void loadNextChunk() {
        try {
            log.debug("다음 청크 로드 시작 - offset: {}, chunkSize: {}", currentOffset, chunkSize);

            // 최근 30일 내 활동 기준
            LocalDate recentActivityThreshold = LocalDate.now().minusDays(30);

            // 청크 단위로 판매자 목록 조회
            List<ForecastBatchItem> items = forecastBatchMapper.findSellersForForecastBatch(
                    recentActivityThreshold, chunkSize, currentOffset);

            if (items.isEmpty()) {
                log.debug("더 이상 로드할 데이터가 없습니다 - offset: {}", currentOffset);
                hasMoreData = false;
                currentIterator = List.<ForecastBatchItem>of().iterator();
                return;
            }

            log.debug("청크 로드 완료 - 로드된 아이템 수: {}, offset: {}", items.size(), currentOffset);

            // Iterator 설정
            currentIterator = items.iterator();

            // 다음 청크를 위한 offset 업데이트
            currentOffset += items.size();

            // 로드된 아이템이 chunkSize보다 적으면 마지막 청크
            if (items.size() < chunkSize) {
                hasMoreData = false;
                log.debug("마지막 청크 로드 완료");
            }

        } catch (Exception e) {
            log.error("청크 로드 실패 - offset: {}", currentOffset, e);
            hasMoreData = false;
            currentIterator = List.<ForecastBatchItem>of().iterator();
            throw new RuntimeException("청크 로드 중 오류 발생", e);
        }
    }

    /**
     * Reader 상태 조회 (모니터링용)
     */
    public ReaderStatus getReaderStatus() {
        return ReaderStatus.builder()
                .totalItemCount(totalItemCount)
                .processedItemCount(processedItemCount)
                .currentOffset(currentOffset)
                .hasMoreData(hasMoreData)
                .progressPercentage(totalItemCount > 0 ?
                        (double) processedItemCount / totalItemCount * 100 : 0.0)
                .build();
    }

    /**
     * Reader 상태 정보
     */
    @lombok.Builder
    @lombok.Getter
    public static class ReaderStatus {
        private final long totalItemCount;
        private final long processedItemCount;
        private final int currentOffset;
        private final boolean hasMoreData;
        private final double progressPercentage;

        @Override
        public String toString() {
            return String.format("Reader 상태 - 진행률: %.1f%% (%d/%d), 오프셋: %d, 더보기: %s",
                    progressPercentage, processedItemCount, totalItemCount, currentOffset, hasMoreData);
        }
    }

    /**
     * Reader 초기화 (Step 재시작시 사용)
     */
    public void resetReader() {
        log.info("ItemReader 상태 초기화");

        currentIterator = null;
        currentOffset = 0;
        hasMoreData = true;
        totalItemCount = 0;
        processedItemCount = 0;
    }
}