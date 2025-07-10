package com.team5.catdogeats.forecast.mapper;

import com.team5.catdogeats.forecast.domain.DailySalesAggregation;
import com.team5.catdogeats.forecast.domain.dto.DailySalesDataDTO;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.apache.ibatis.annotations.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DailySalesAggregationMapper {

    /**
     * 일별 판매 집계 데이터 저장 (타임스탬프 필드 제거 - DB DEFAULT 사용)
     */
    @Insert("""
        INSERT INTO daily_sales_aggregation (
            id, seller_id, product_id, sales_date,
            daily_quantity, daily_revenue, order_count
        ) VALUES (
            #{id}, #{seller.userId}, #{product.id}, #{salesDate},
            #{dailyQuantity}, #{dailyRevenue}, #{orderCount}
        ) ON CONFLICT (seller_id, product_id, sales_date) 
        DO UPDATE SET 
            daily_quantity = #{dailyQuantity},
            daily_revenue = #{dailyRevenue},
            order_count = #{orderCount}
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
     * Seller 조회 (타임스탬프 필드 제외 - BaseEntity 필드 충돌 방지)
     */
    @Select("""
        SELECT 
            s.user_id,
            s.vendor_name,
            s.vendor_profile_image,
            s.business_number,
            s.settlement_bank,
            s.settlement_acc as settlement_account,
            s.tags,
            s.operating_start_time,
            s.operating_end_time,
            s.closed_days,
            s.is_deleted,
            s.deleted_at
        FROM sellers s
        WHERE s.user_id = #{sellerId}
        """)
    @Results({
            @Result(property = "userId", column = "user_id"),
            @Result(property = "vendorName", column = "vendor_name"),
            @Result(property = "vendorProfileImage", column = "vendor_profile_image"),
            @Result(property = "businessNumber", column = "business_number"),
            @Result(property = "settlementBank", column = "settlement_bank"),
            @Result(property = "settlementAccount", column = "settlement_account"),
            @Result(property = "tags", column = "tags"),
            @Result(property = "operatingStartTime", column = "operating_start_time"),
            @Result(property = "operatingEndTime", column = "operating_end_time"),
            @Result(property = "closedDays", column = "closed_days"),
            @Result(property = "isDeleted", column = "is_deleted"),
            @Result(property = "deledAt", column = "deleted_at")
            // createdAt, updatedAt 제외 - BaseEntity 타임스탬프 충돌 방지
    })
    Sellers findSellerById(@Param("sellerId") String sellerId);

    /**
     * Product 조회 (실제 DB 컬럼명으로 수정)
     */
    @Select("""
        SELECT 
            p.id,
            p.product_number,
            p.seller_id,
            p.title,
            p.subtitle,
            p.productinfo,
            p.contents,
            p.petcategory,
            p.productcategory,
            p.stock_status,
            p.price,
            p.discount_rate,
            p.is_discounted,
            p.stock,
            p.lead_time,
            p.version
        FROM products p
        WHERE p.id = #{productId}
        """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "productNumber", column = "product_number"),
            @Result(property = "seller.userId", column = "seller_id"),
            @Result(property = "title", column = "title"),
            @Result(property = "subTitle", column = "subtitle"),  // 수정: subtitle
            @Result(property = "productInfo", column = "productinfo"), // 수정: productinfo
            @Result(property = "contents", column = "contents"),
            @Result(property = "petCategory", column = "petcategory"),
            @Result(property = "productCategory", column = "productcategory"),
            @Result(property = "stockStatus", column = "stock_status"),
            @Result(property = "price", column = "price"),
            @Result(property = "discountRate", column = "discount_rate"),
            @Result(property = "isDiscounted", column = "is_discounted"),
            @Result(property = "stock", column = "stock"),
            @Result(property = "leadTime", column = "lead_time"),
            @Result(property = "version", column = "version")
            // createdAt, updatedAt 제외 - BaseEntity 타임스탬프 충돌 방지
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

// ==========================================================
// 대안: TypeHandler를 사용한 ZonedDateTime 처리 방식
// ==========================================================

/*
만약 ZonedDateTime을 계속 사용하고 싶다면 다음 TypeHandler를 등록하세요:

@Component
public class ZonedDateTimeTypeHandler extends BaseTypeHandler<ZonedDateTime> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ZonedDateTime parameter, JdbcType jdbcType) throws SQLException {
        ps.setTimestamp(i, Timestamp.valueOf(parameter.toLocalDateTime()));
    }

    @Override
    public ZonedDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnName);
        return timestamp != null ? ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault()) : null;
    }

    @Override
    public ZonedDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(columnIndex);
        return timestamp != null ? ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault()) : null;
    }

    @Override
    public ZonedDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Timestamp timestamp = cs.getTimestamp(columnIndex);
        return timestamp != null ? ZonedDateTime.ofInstant(timestamp.toInstant(), ZoneId.systemDefault()) : null;
    }
}

그리고 MyBatis 설정에서 등록:
@Configuration
public class MyBatisConfig {
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        // TypeHandler 등록
        factory.setTypeHandlers(new TypeHandler[]{new ZonedDateTimeTypeHandler()});

        return factory.getObject();
    }
}
*/