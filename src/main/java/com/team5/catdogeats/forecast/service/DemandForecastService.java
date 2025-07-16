package com.team5.catdogeats.forecast.service;

import com.team5.catdogeats.forecast.domain.dto.DemandForecastResultDTO;

import java.time.LocalDate;
import java.util.List;

/**
 * 수요예측 서비스 인터페이스
 */
public interface DemandForecastService {

    /**
     * 특정 판매자의 모든 상품에 대한 수요예측 실행
     * @param sellerId 판매자 ID
     * @return 예측 완료된 상품 수
     */
    int executeForecasting(String sellerId);

    /**
     * 특정 판매자의 최신 수요예측 결과 조회 (재고 부족량 포함)
     * @param sellerId 판매자 ID
     * @return 수요예측 결과 목록 (부족량 순으로 정렬)
     */
    List<DemandForecastResultDTO> getLatestForecastResults(String sellerId);

    /**
     * 오래된 예측 데이터 정리
     * @param cutoffDate 삭제 기준일 (이전 데이터 삭제)
     * @return 삭제된 레코드 수
     */
    int cleanupOldForecasts(LocalDate cutoffDate);
}
