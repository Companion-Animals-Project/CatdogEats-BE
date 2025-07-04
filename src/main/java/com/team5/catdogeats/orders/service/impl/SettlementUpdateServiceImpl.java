package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.global.annotation.MybatisTransactional;
import com.team5.catdogeats.orders.mapper.SettlementUpdateMapper;
import com.team5.catdogeats.orders.service.SettlementUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 정산 데이터 업데이트 서비스 구현체
 * 스케줄러에서 호출되는 정산 갱신 로직 처리
 * 완전 테이블 기반 방식으로 Settlements 테이블을 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementUpdateServiceImpl implements SettlementUpdateService {

    private final SettlementUpdateMapper settlementUpdateMapper;

    @Override
    @MybatisTransactional
    public long createSettlementsForYesterday() {
        log.info("어제 주문의 정산 데이터 생성 시작");

        try {
            // 어제 생성된 주문들의 정산 데이터 생성
            LocalDate yesterday = LocalDate.now().minusDays(1);
            long createdCount = settlementUpdateMapper.insertDailySettlements(yesterday);

            log.info("어제 주문의 정산 데이터 생성 완료 - 생성된 건수: {}, 대상일: {}",
                    createdCount, yesterday);

            return createdCount;

        } catch (Exception e) {
            log.error("어제 주문의 정산 데이터 생성 중 오류", e);
            throw new RuntimeException("어제 주문의 정산 데이터 생성 실패", e);
        }
    }

    @Override
    @MybatisTransactional
    public long updateSettlementStatuses() {
        log.info("배송 상태에 따른 정산 상태 갱신 시작");

        try {
            // 배송완료 후 7일 경과 시 대기중 → 처리중으로 변경
            long updatedCount = settlementUpdateMapper.updateSettlementStatuses();

            log.info("배송 상태에 따른 정산 상태 갱신 완료 - 갱신된 건수: {}", updatedCount);

            return updatedCount;

        } catch (Exception e) {
            log.error("배송 상태에 따른 정산 상태 갱신 중 오류", e);
            throw new RuntimeException("정산 상태 갱신 실패", e);
        }
    }

    @Override
    @MybatisTransactional
    public long completeLastMonthSettlements() {
        log.info("지난달 정산 완료 처리 시작");

        try {
            // 지난달의 처리중 상태를 정산완료로 변경
            YearMonth lastMonth = YearMonth.now().minusMonths(1);
            long completedCount = settlementUpdateMapper.completeLastMonthSettlements(lastMonth);

            log.info("지난달 정산 완료 처리 완료 - 완료 처리 건수: {}, 대상월: {}",
                    completedCount, lastMonth);

            return completedCount;

        } catch (Exception e) {
            log.error("지난달 정산 완료 처리 중 오류", e);
            throw new RuntimeException("지난달 정산 완료 처리 실패", e);
        }
    }
}

