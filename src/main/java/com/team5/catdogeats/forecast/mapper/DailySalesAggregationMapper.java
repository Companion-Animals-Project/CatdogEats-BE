package com.team5.catdogeats.forecast.mapper;

import com.team5.catdogeats.forecast.domain.DailySalesAggregation;
import com.team5.catdogeats.forecast.domain.dto.DailySalesDataDTO;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.apache.ibatis.annotations.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailySalesAggregationMapper {

    /**
     * 일별 판매 집계 데이터 저장
     */
    @Insert("""
    INSERT INTO daily_sales_aggregation (
        id, seller_id, product_id, sales_date,
        daily_quantity, daily_revenue, order_count,
        created_at, updated_at
    ) VALUES (
        #{id}, #{seller.userId}, #{product.id}, #{salesDate},
        #{dailyQuantity}, #{dailyRevenue}, #{orderCount},
        #{createdAt}, #{updatedAt}
    ) ON CONFLICT (seller_id, product_id, sales_date) 
    DO UPDATE SET 
        daily_quantity = #{dailyQuantity},
        daily_revenue = #{dailyRevenue},
        order_count = #{orderCount},
        updated_at = #{updatedAt}
    """)
    void upsertDailySales(DailySalesAggregation aggregation);


    /**
     * 원본 주문 데이터에서 일별 판매 집계 생성 (배치용)
     * Record 생성자 매핑을 사용하여 DailySalesDataDTO 객체 생성
     */
    @Select("""
        SELECT 
            p.seller_id,
            oi.product_id,
            DATE(o.created_at) as sales_date,
            SUM(oi.quantity) as daily_quantity,
            SUM(oi.quantity * oi.price) as daily_revenue,
            COUNT(DISTINCT o.id) as order_count
        FROM orders o
        INNER JOIN order_items oi ON o.id = oi.order_id
        INNER JOIN products p ON oi.product_id = p.id
        WHERE DATE(o.created_at) = #{targetDate}
        AND o.order_status NOT IN ('CANCELLED', 'REFUNDED')
        AND o.is_hidden = false
        GROUP BY p.seller_id, oi.product_id, DATE(o.created_at)
        """)
    @ConstructorArgs({
            @Arg(column = "seller_id", javaType = String.class),      // 1. sellerId
            @Arg(column = "product_id", javaType = String.class),     // 2. productId
            @Arg(column = "sales_date", javaType = LocalDate.class),  // 3. salesDate
            @Arg(column = "daily_quantity", javaType = Integer.class), // 4. dailyQuantity
            @Arg(column = "daily_revenue", javaType = Long.class),    // 5. dailyRevenue
            @Arg(column = "order_count", javaType = Integer.class)    // 6. orderCount
    })
    List<DailySalesDataDTO> aggregateDailySalesByDate(@Param("targetDate") LocalDate targetDate);

    /**
     * Seller 조회
     */
    @Select("""
        SELECT s.user_id
        FROM sellers s
        WHERE s.user_id = #{sellerId}
        """)
    @Results({
            @Result(property = "userId", column = "user_id")
    })
    Sellers findSellerById(@Param("sellerId") String sellerId);

    /**
     * Product
     */
    @Select("""
        SELECT p.id
        FROM products p
        WHERE p.id = #{productId}
        """)
    @Results({
            @Result(property = "id", column = "id")
    })
    Products findProductById(@Param("productId") String productId);

    /**
     * 특정 상품의 최근 N일간 판매 데이터 조회 (예측 계산용)
     * 모든 필드를 조회해서 Record 생성자 파라미터 맞춤
     */
    @Select("""
        SELECT 
            #{sellerId} as seller_id,
            #{productId} as product_id,
            sales_date,
            daily_quantity,
            0 as daily_revenue,
            0 as order_count
        FROM daily_sales_aggregation
        WHERE seller_id = #{sellerId} 
        AND product_id = #{productId}
        AND sales_date BETWEEN #{startDate} AND #{endDate}
        ORDER BY sales_date ASC
        """)
    @ConstructorArgs({
            @Arg(column = "seller_id", javaType = String.class),
            @Arg(column = "product_id", javaType = String.class),
            @Arg(column = "sales_date", javaType = LocalDate.class),
            @Arg(column = "daily_quantity", javaType = Integer.class),
            @Arg(column = "daily_revenue", javaType = Long.class),
            @Arg(column = "order_count", javaType = Integer.class)
    })
    List<DailySalesDataDTO> findSalesDataForForecast(
            @Param("sellerId") String sellerId,
            @Param("productId") String productId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 최근 30일간 판매 데이터가 충분한 상품 목록 조회
     */
    @Select("""
        SELECT product_id
        FROM daily_sales_aggregation
        WHERE seller_id = #{sellerId} 
        AND sales_date >= #{startDate}
        AND daily_quantity > 0
        GROUP BY product_id
        HAVING COUNT(DISTINCT sales_date) >= #{minDays}
        """)
    List<String> findProductsWithSufficientData(
            @Param("sellerId") String sellerId,
            @Param("startDate") LocalDate startDate,
            @Param("minDays") Integer minDays);

    /**
     * 기존 집계 데이터 존재 여부 확인
     */
    @Select("""
        SELECT COUNT(*) > 0 
        FROM daily_sales_aggregation
        WHERE seller_id = #{sellerId} 
        AND product_id = #{productId} 
        AND sales_date = #{salesDate}
        """)
    boolean existsBySellerAndProductAndDate(
            @Param("sellerId") String sellerId,
            @Param("productId") String productId,
            @Param("salesDate") LocalDate salesDate);
}
