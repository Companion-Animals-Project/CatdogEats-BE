package com.team5.catdogeats.orders.service;

/**
 * 정산 데이터 업데이트 서비스 인터페이스
 * 스케줄러에서 사용하는 정산 데이터 갱신 기능
 */
public interface SettlementUpdateService {

    /**
     * 어제 주문의 정산 데이터 생성
     * 어제 생성된 주문들의 정산 정보를 Settlements 테이블에 생성
     *
     * @return 생성된 정산 건수
     */
    long createSettlementsForYesterday();

    /**
     * 배송 상태에 따른 정산 상태 갱신
     * 배송완료 후 7일 경과 시 대기중 → 처리중으로 변경
     *
     * @return 갱신된 정산 건수
     */
    long updateSettlementStatuses();

    /**
     * 지난달 정산 완료 처리
     * 지난달의 처리중 상태를 정산완료로 변경
     *
     * @return 완료 처리된 건수
     */
    long completeLastMonthSettlements();
}
