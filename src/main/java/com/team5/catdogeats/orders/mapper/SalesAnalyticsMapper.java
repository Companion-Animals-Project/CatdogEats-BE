package com.team5.catdogeats.orders.mapper;

import com.team5.catdogeats.orders.domain.dto.MonthlySalesRawDataDTO;
import com.team5.catdogeats.orders.domain.dto.ProductSalesRawDataDTO;
import com.team5.catdogeats.orders.domain.dto.TotalSalesAmountDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 매출 분석 조회를
 */
@Mapper
public interface SalesAnalyticsMapper {



    /**
     * 특정 판매자의 특정 년도 월별 매출 집계 조회
     * 1~12월 데이터를 모두 조회 (데이터가 없는 월은 0으로 처리)
     */
    @Select("""
        WITH months AS (
            SELECT generate_series(1, 12) AS month
        ),
        monthly_sales AS (
            SELECT 
                sales_month,
                SUM(total_amount) as total_amount,
                COUNT(DISTINCT order_number) as order_count,
                SUM(quantity) as total_quantity
            FROM v_sales_analytics
            WHERE seller_id = #{sellerId} 
                AND sales_year = #{year}
            GROUP BY sales_month
        )
        SELECT 
            m.month,
            COALESCE(ms.total_amount, 0) as total_amount,
            COALESCE(ms.order_count, 0) as order_count,
            COALESCE(ms.total_quantity, 0) as total_quantity
        FROM months m
        LEFT JOIN monthly_sales ms ON m.month = ms.sales_month
        ORDER BY m.month
        """)
    @ConstructorArgs({
            @Arg(column = "month", javaType = Integer.class),
            @Arg(column = "total_amount", javaType = Long.class),
            @Arg(column = "order_count", javaType = Long.class),
            @Arg(column = "total_quantity", javaType = Long.class)
    })
    List<MonthlySalesRawDataDTO> findMonthlySalesBySellerAndYear(
            @Param("sellerId") String sellerId,
            @Param("year") Integer year
    );

    /**
     * 특정 판매자의 특정 년도 총 매출 요약 조회
     */
    @Select("""
        SELECT 
            COALESCE(SUM(total_amount), 0) as total_amount,
            COUNT(DISTINCT order_number) as order_count,
            COALESCE(SUM(quantity), 0) as total_quantity
        FROM v_sales_analytics
        WHERE seller_id = #{sellerId} 
            AND sales_year = #{year}
        """)
    @ConstructorArgs({
            @Arg(column = "total_amount", javaType = Long.class),
            @Arg(column = "order_count", javaType = Long.class),
            @Arg(column = "total_quantity", javaType = Long.class)
    })
    MonthlySalesRawDataDTO findYearTotalSalesBySellerAndYear(
            @Param("sellerId") String sellerId,
            @Param("year") Integer year
    );


    /**
     * 특정 판매자의 연도별 상품별 매출 조회 (페이징)
     */
    @Select("""
        SELECT 
            product_id,
            product_name,
            SUM(total_amount) as total_amount,
            SUM(quantity) as quantity
        FROM v_sales_analytics
        WHERE seller_id = #{sellerId} 
            AND sales_year = #{year}
        GROUP BY product_id, product_name
        ORDER BY total_amount DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    @ConstructorArgs({
            @Arg(column = "product_id", javaType = String.class),
            @Arg(column = "product_name", javaType = String.class),
            @Arg(column = "total_amount", javaType = Long.class),
            @Arg(column = "quantity", javaType = Long.class)
    })
    List<ProductSalesRawDataDTO> findYearlyProductSalesBySellerAndYear(
            @Param("sellerId") String sellerId,
            @Param("year") Integer year,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    /**
     * 특정 판매자의 월별 상품별 매출 조회 (페이징)
     */
    @Select("""
        SELECT 
            product_id,
            product_name,
            SUM(total_amount) as total_amount,
            SUM(quantity) as quantity
        FROM v_sales_analytics
        WHERE seller_id = #{sellerId} 
            AND sales_year = #{year}
            AND sales_month = #{month}
        GROUP BY product_id, product_name
        ORDER BY total_amount DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    @ConstructorArgs({
            @Arg(column = "product_id", javaType = String.class),
            @Arg(column = "product_name", javaType = String.class),
            @Arg(column = "total_amount", javaType = Long.class),
            @Arg(column = "quantity", javaType = Long.class)
    })
    List<ProductSalesRawDataDTO> findMonthlyProductSalesBySellerAndYearMonth(
            @Param("sellerId") String sellerId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    /**
     * 특정 판매자의 연도별 상품 개수 조회 (페이징용)
     */
    @Select("""
        SELECT COUNT(DISTINCT product_id)
        FROM v_sales_analytics
        WHERE seller_id = #{sellerId} 
            AND sales_year = #{year}
        """)
    Long countYearlyProductsBySellerAndYear(
            @Param("sellerId") String sellerId,
            @Param("year") Integer year
    );

    /**
     * 특정 판매자의 월별 상품 개수 조회 (페이징용)
     */
    @Select("""
        SELECT COUNT(DISTINCT product_id)
        FROM v_sales_analytics
        WHERE seller_id = #{sellerId} 
            AND sales_year = #{year}
            AND sales_month = #{month}
        """)
    Long countMonthlyProductsBySellerAndYearMonth(
            @Param("sellerId") String sellerId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

    /**
     * 특정 판매자의 연도별 총 매출액 조회 (퍼센트 계산용)
     */
    @Select("""
        SELECT COALESCE(SUM(total_amount), 0) as total_amount
        FROM v_sales_analytics
        WHERE seller_id = #{sellerId} 
            AND sales_year = #{year}
        """)
    @ConstructorArgs({
            @Arg(column = "total_amount", javaType = Long.class)
    })
    TotalSalesAmountDTO findYearlyTotalAmountBySellerAndYear(
            @Param("sellerId") String sellerId,
            @Param("year") Integer year
    );

    /**
     * 특정 판매자의 월별 총 매출액 조회 (퍼센트 계산용)
     */
    @Select("""
        SELECT COALESCE(SUM(total_amount), 0) as total_amount
        FROM v_sales_analytics
        WHERE seller_id = #{sellerId} 
            AND sales_year = #{year}
            AND sales_month = #{month}
        """)
    @ConstructorArgs({
            @Arg(column = "total_amount", javaType = Long.class)
    })
    TotalSalesAmountDTO findMonthlyTotalAmountBySellerAndYearMonth(
            @Param("sellerId") String sellerId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );

}