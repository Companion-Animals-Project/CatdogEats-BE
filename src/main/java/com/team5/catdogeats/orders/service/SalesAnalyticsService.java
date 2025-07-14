package com.team5.catdogeats.orders.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.dto.PeriodSalesAnalyticsResponseDTO;
import com.team5.catdogeats.orders.domain.dto.ProductSalesAnalyticsRequestDTO;
import com.team5.catdogeats.orders.domain.dto.ProductSalesAnalyticsResponseDTO;
import org.springframework.data.domain.Pageable;

/**
 * 매출 분석 서비스 인터페이스
 */
public interface SalesAnalyticsService {

    /**
     * 기간별 매출 분석 조회 (년도별 월별 집계)
     */
    PeriodSalesAnalyticsResponseDTO getPeriodSalesAnalytics(UserPrincipal userPrincipal, Integer year);


    /**
     * 상품별 매출 분석 조회 (연도별/월별 + 페이징)
     */
    ProductSalesAnalyticsResponseDTO getProductSalesAnalytics(
            UserPrincipal userPrincipal,
            ProductSalesAnalyticsRequestDTO request,
            Pageable pageable
    );
}