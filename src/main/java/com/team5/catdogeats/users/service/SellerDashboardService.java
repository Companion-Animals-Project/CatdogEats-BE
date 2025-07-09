package com.team5.catdogeats.users.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.dto.SellerDashboardResponseDTO;

/**
 * 판매자 대시보드 서비스 인터페이스
 * 판매자의 대시보드에 필요한 통계 데이터를 제공하는 서비스
 */
public interface SellerDashboardService {

    /**
     * 판매자 대시보드 데이터 조회
     * 오늘 주문 통계, 주간 매출 동향, 이번 달 상품 매출 순위를 포함한 종합 대시보드 데이터를 제공합니다.
     */
    SellerDashboardResponseDTO getDashboardData(UserPrincipal userPrincipal);
}
