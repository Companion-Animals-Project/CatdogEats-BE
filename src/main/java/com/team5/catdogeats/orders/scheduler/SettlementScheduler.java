package com.team5.catdogeats.orders.scheduler;

import com.team5.catdogeats.orders.service.SettlementUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정산 관련 스케줄러
 * 1. 매일 새벽 2시: 어제 주문의 정산 데이터 생성 + 배송상태 기반 정산상태 갱신
 * 2. 매월 1일 새벽 3시: 처리중 상태를 정산완료로 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementScheduler {

    private final SettlementUpdateService settlementUpdateService;

    /**
     * 매일 새벽 2시에 정산 데이터 관리
     * 1) 어제 생성된 주문들의 정산 데이터 생성
     * 2) 배송 상태 변경에 따른 정산 상태 갱신
     */
    @Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
    public void updateDailySettlements() {
        log.info("일일 정산 데이터 갱신 스케줄러 시작");

        try {
            // 1. 어제 주문의 정산 데이터 생성
            long createdCount = settlementUpdateService.createSettlementsForYesterday();

            // 2. 배송 상태에 따른 정산 상태 갱신 (대기중 → 처리중)
            long updatedCount = settlementUpdateService.updateSettlementStatuses();

            log.info("일일 정산 데이터 갱신 완료 - 신규 생성: {}, 상태 갱신: {}",
                    createdCount, updatedCount);

        } catch (Exception e) {
            log.error("일일 정산 데이터 갱신 중 오류 발생", e);
        }
    }

    /**
     * 매월 1일 새벽 3시에 처리중 상태를 정산완료로 변경
     * 지난달 처리중인 모든 정산을 정산완료 상태로 변경
     */
    @Scheduled(cron = "0 0 3 1 * *") // 매월 1일 새벽 3시
    public void completeMonthlySettlements() {
        log.info("월별 정산 완료 처리 스케줄러 시작");

        try {
            long completedCount = settlementUpdateService.completeLastMonthSettlements();
            log.info("월별 정산 완료 처리 완료 - 완료 처리된 건수: {}", completedCount);

        } catch (Exception e) {
            log.error("월별 정산 완료 처리 중 오류 발생", e);
        }
    }
}