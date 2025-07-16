package com.team5.catdogeats.users.mapper;

import com.team5.catdogeats.users.domain.dto.TodayStatsDTO;
import com.team5.catdogeats.users.domain.dto.WeeklySalesDTO;
import com.team5.catdogeats.users.domain.dto.ProductSalesRankingDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 판매자 대시보드 데이터 조회를 위한 MyBatis Mapper (어노테이션 방식)
 * 실시간 주문 데이터 기반으로 대시보드 통계 제공
 */
@Mapper
public interface SellerDashboardMapper {

    /**
     * 오늘 주문 통계 조회 (오늘 주문 수, 오늘 총 매출액)
     * 취소/환불/숨김 처리된 주문은 제외
     */
    @Select("""
        WITH today_orders AS (
            SELECT DISTINCT o.id as order_id
            FROM orders o
            INNER JOIN order_items oi ON o.id = oi.order_id
            INNER JOIN products p ON oi.product_id = p.id
            WHERE p.seller_id = #{sellerId}
                AND DATE(o.created_at) = CURRENT_DATE
                AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')
                AND o.is_hidden = false
        ),
        today_sales AS (
            SELECT 
                COALESCE(SUM(oi.quantity * oi.price), 0) as total_sales
            FROM orders o
            INNER JOIN order_items oi ON o.id = oi.order_id
            INNER JOIN products p ON oi.product_id = p.id
            WHERE p.seller_id = #{sellerId}
                AND DATE(o.created_at) = CURRENT_DATE
                AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')
                AND o.is_hidden = false
        )
        SELECT 
            (SELECT COUNT(*) FROM today_orders) as todayOrderCount,
            (SELECT total_sales FROM today_sales) as todayTotalSales
        """)
    @ConstructorArgs({
            @Arg(column = "todayOrderCount", javaType = Long.class),
            @Arg(column = "todayTotalSales", javaType = Long.class)
    })
    TodayStatsDTO findTodayStats(@Param("sellerId") String sellerId);

    /**
     * 주간 매출 동향 조회 (이번 주 7일간 일별 매출)
     * 월요일부터 일요일까지의 매출 데이터를 조회
     * 매출이 없는 날은 0으로 표시
     * 판매자별 실제 매출만 계산 (혼합 주문 고려)
     */
    @Select("""
        WITH week_dates AS (
            SELECT 
                (DATE_TRUNC('week', CURRENT_DATE) + (i || ' days')::interval)::date as sales_date
            FROM generate_series(0, 6) i
        ),
        daily_sales AS (
            SELECT 
                DATE(o.created_at) as sales_date,
                COALESCE(SUM(oi.quantity * oi.price), 0) as daily_sales
            FROM orders o
            INNER JOIN order_items oi ON o.id = oi.order_id
            INNER JOIN products p ON oi.product_id = p.id
            WHERE p.seller_id = #{sellerId}
                AND o.created_at >= DATE_TRUNC('week', CURRENT_DATE)
                AND o.created_at < DATE_TRUNC('week', CURRENT_DATE) + INTERVAL '7 days'
                AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')
                AND o.is_hidden = false
            GROUP BY DATE(o.created_at)
        )
        SELECT 
            wd.sales_date::text as salesDate,
            COALESCE(ds.daily_sales, 0) as dailySales
        FROM week_dates wd
        LEFT JOIN daily_sales ds ON wd.sales_date = ds.sales_date
        ORDER BY wd.sales_date
        """)
    @ConstructorArgs({
            @Arg(column = "salesDate", javaType = String.class),
            @Arg(column = "dailySales", javaType = Long.class)
    })
    List<WeeklySalesDTO> findWeeklySales(@Param("sellerId") String sellerId);

    /**
     * 이번 달 상품 매출 순위 조회 (실시간, TOP 10)
     * 취소/환불/숨김 처리된 주문은 제외
     */
    @Select("""
        SELECT 
            p.id as productId,
            p.title as productName,
            SUM(oi.quantity) as totalQuantity,
            SUM(oi.quantity * oi.price) as totalSales
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
        WHERE p.seller_id = #{sellerId}
            AND EXTRACT(YEAR FROM o.created_at) = EXTRACT(YEAR FROM CURRENT_DATE)
            AND EXTRACT(MONTH FROM o.created_at) = EXTRACT(MONTH FROM CURRENT_DATE)
            AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')
            AND o.is_hidden = false
        GROUP BY p.id, p.title
        ORDER BY totalSales DESC
        LIMIT 10
        """)
    @ConstructorArgs({
            @Arg(column = "productId", javaType = String.class),
            @Arg(column = "productName", javaType = String.class),
            @Arg(column = "totalQuantity", javaType = Long.class),
            @Arg(column = "totalSales", javaType = Long.class)
    })
    List<ProductSalesRankingDTO> findMonthlyProductRanking(@Param("sellerId") String sellerId);
}