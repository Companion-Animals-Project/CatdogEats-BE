package com.team5.catdogeats.users.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.forecast.domain.dto.DemandForecastResultDTO;
import com.team5.catdogeats.users.domain.dto.SellerDashboardResponseDTO;

import java.util.List;

/**
 * 판매자 대시보드 서비스 인터페이스 (수요예측 포함)
 * 판매자의 대시보드에 필요한 통계 데이터 및 수요예측을 제공하는 서비스
 */
public interface SellerDashboardService {

    /**
     * 판매자 대시보드 데이터 조회 (수요예측 포함)
     * 오늘 주문 통계, 주간 매출 동향, 이번 달 상품 매출 순위, 수요예측 결과를 포함한 종합 대시보드 데이터를 제공합니다.
     */
    SellerDashboardResponseDTO getDashboardData(UserPrincipal userPrincipal);

    /**
     * 판매자의 수요예측 결과만 조회
     * @param userPrincipal 인증된 판매자 정보
     * @return 수요예측 결과 목록 (재고 부족량 순으로 정렬)
     */
    List<DemandForecastResultDTO> getDemandForecastResults(UserPrincipal userPrincipal);
}
