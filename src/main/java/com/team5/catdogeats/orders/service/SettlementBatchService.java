package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.global.annotation.MybatisTransactional;
import com.team5.catdogeats.orders.mapper.SettlementBatchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 정산 데이터 생성/갱신 배치 서비스
 * 기존 SettlementService와 구분하여 배치 전용 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@MybatisTransactional
public class SettlementBatchService {

    private final SettlementBatchMapper settlementBatchMapper;

    // 플랫폼 수수료율 (설정값으로 분리 가능)
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.1"); // 10%

    /**
     * 정산 데이터 생성
     * 배송 완료된 주문 아이템들에 대해 정산 데이터를 생성합니다.
     */

    public void createSettlements() {
        log.info("정산 데이터 생성 배치 시작");

        try {
            // 배송완료되었지만 정산이 생성되지 않은 주문 아이템들에 대해 정산 생성
            int createdCount = settlementBatchMapper.createSettlementsForDeliveredItems(DEFAULT_COMMISSION_RATE);

            log.info("정산 데이터 생성 배치 완료 - 생성된 정산 건수: {}", createdCount);
        } catch (Exception e) {
            log.error("정산 데이터 생성 배치 실패", e);
            throw e;
        }
    }

    /**
     * 정산 상태 갱신 (PENDING -> IN_PROGRESS)
     * 배송완료 후 7일이 경과한 정산을 처리중으로 변경합니다.
     */

    public void updateSettlementsToInProgress() {
        log.info("정산 상태 갱신 배치 시작 (PENDING -> IN_PROGRESS)");

        try {
            // 배송완료 후 7일 경과한 정산들을 처리중으로 변경
            int updatedCount = settlementBatchMapper.updatePendingToInProgress();

            log.info("정산 상태 갱신 배치 완료 - IN_PROGRESS로 변경된 정산 건수: {}", updatedCount);
        } catch (Exception e) {
            log.error("정산 상태 갱신 배치 실패", e);
            throw e;
        }
    }

    /**
     * 정산 완료 처리 (IN_PROGRESS -> COMPLETED)
     * 매월 1일에 처리중인 정산들을 정산완료로 변경합니다.
     */

    public void completeSettlements() {
        log.info("정산 완료 처리 배치 시작 (IN_PROGRESS -> COMPLETED)");

        try {
            // 처리중인 정산들을 정산완료로 변경
            int completedCount = settlementBatchMapper.updateInProgressToCompleted();

            log.info("정산 완료 처리 배치 완료 - COMPLETED로 변경된 정산 건수: {}", completedCount);
        } catch (Exception e) {
            log.error("정산 완료 처리 배치 실패", e);
            throw e;
        }
    }

    /**
     * 정산 대상 건수 조회 (모니터링용)
     */
    public int getUnsettledItemsCount() {
        return settlementBatchMapper.countUnsettledItems();
    }

    /**
     * 처리중 정산 건수 조회 (모니터링용)
     */
    public int getInProgressSettlementsCount() {
        return settlementBatchMapper.countInProgressSettlements();
    }

    /**
     * 대기중 정산 중 7일 경과된 건수 조회 (모니터링용)
     */
    public int getPendingSettlementsReadyForProgressCount() {
        return settlementBatchMapper.countPendingSettlementsReadyForProgress();
    }
}