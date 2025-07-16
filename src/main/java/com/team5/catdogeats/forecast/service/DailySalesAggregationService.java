package com.team5.catdogeats.forecast.service;

import java.time.LocalDate;

/**
 * 일별 판매 데이터 집계 서비스 인터페이스
 * 원본 주문 데이터를 기반으로 판매량을 일별로 집계하여 저장하는 서비스
 *
 */
public interface DailySalesAggregationService {

    /**
     * 특정 날짜의 일별 판매 데이터 집계
     */
    int aggregateDailySales(LocalDate targetDate);
}