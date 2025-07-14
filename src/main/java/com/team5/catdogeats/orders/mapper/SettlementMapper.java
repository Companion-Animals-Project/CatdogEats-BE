package com.team5.catdogeats.orders.mapper;

import com.team5.catdogeats.orders.domain.dto.MonthlySettlementStatusDto;
import com.team5.catdogeats.orders.domain.dto.SettlementItemDTO;
import com.team5.catdogeats.orders.domain.dto.SettlementSummaryDTO;
import org.apache.ibatis.annotations.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 정산현황 조회를 위한 MyBatis Mapper (어노테이션 방식)
 * Settlement 테이블 기반으로 정산 데이터 조회 (배송완료 후 7일 지난 데이터만)
 */
@Mapper
public interface SettlementMapper {

    /**
     * 판매자의 전체 정산 리스트 조회 (페이징)
     */
    @Select("""
        SELECT 
            o.order_number,
            p.title as product_name,
            st.item_price as order_amount,
            st.commission_amount as commission,
            st.settlement_amount,
            o.created_at as order_date,
            s.delivered_at as delivery_date,
            st.created_at as settlement_created_at,
            st.settlement_status
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
        LEFT JOIN shipments s ON o.id = s.order_id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        ORDER BY o.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    @ConstructorArgs({
            @Arg(column = "order_number", javaType = String.class),
            @Arg(column = "product_name", javaType = String.class),
            @Arg(column = "order_amount", javaType = Long.class),
            @Arg(column = "commission", javaType = Long.class),
            @Arg(column = "settlement_amount", javaType = Long.class),
            @Arg(column = "order_date", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "delivery_date", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "settlement_created_at", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "settlement_status", javaType = com.team5.catdogeats.orders.domain.enums.SettlementStatus.class)
    })
    List<SettlementItemDTO> findSettlementsBySellerId(
            @Param("sellerId") String sellerId,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    /**
     * 판매자의 전체 정산 건수 조회
     */
    @Select("""
        SELECT COUNT(*)
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        """)
    Long countSettlementsBySellerId(@Param("sellerId") String sellerId);

    /**
     * 판매자의 전체 정산 요약 정보 조회
     */
    @Select("""
        SELECT 
            COUNT(*) as total_count,
            COALESCE(SUM(st.settlement_amount), 0) as total_settlement_amount,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'COMPLETED'
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as completed_amount,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'IN_PROGRESS'
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as inprogress_amount
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        """)
    @ConstructorArgs({
            @Arg(column = "total_count", javaType = Long.class),
            @Arg(column = "total_settlement_amount", javaType = Long.class),
            @Arg(column = "completed_amount", javaType = Long.class),
            @Arg(column = "inprogress_amount", javaType = Long.class)
    })
    SettlementSummaryDTO getSettlementSummaryBySellerId(@Param("sellerId") String sellerId);

    /**
     * 판매자의 기간별 정산 리스트 조회 (페이징)
     */
    @Select("""
        SELECT 
            o.order_number,
            p.title as product_name,
            st.item_price as order_amount,
            st.commission_amount as commission,
            st.settlement_amount,
            o.created_at as order_date,
            s.delivered_at as delivery_date,
            st.created_at as settlement_created_at,
            st.settlement_status
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
        LEFT JOIN shipments s ON o.id = s.order_id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND DATE(st.created_at) BETWEEN #{startDate} AND #{endDate}
        ORDER BY st.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    @ConstructorArgs({
            @Arg(column = "order_number", javaType = String.class),
            @Arg(column = "product_name", javaType = String.class),
            @Arg(column = "order_amount", javaType = Long.class),
            @Arg(column = "commission", javaType = Long.class),
            @Arg(column = "settlement_amount", javaType = Long.class),
            @Arg(column = "order_date", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "delivery_date", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "settlement_created_at", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "settlement_status", javaType = com.team5.catdogeats.orders.domain.enums.SettlementStatus.class)
    })
    List<SettlementItemDTO> findSettlementsBySellerIdAndPeriod(
            @Param("sellerId") String sellerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    /**
     * 판매자의 기간별 정산 건수 조회
     */
    @Select("""
        SELECT COUNT(*)
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND DATE(st.created_at) BETWEEN #{startDate} AND #{endDate}
        """)
    Long countSettlementsBySellerIdAndPeriod(
            @Param("sellerId") String sellerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 판매자의 기간별 정산 요약 정보 조회
     */
    @Select("""
        SELECT 
            COUNT(*) as total_count,
            COALESCE(SUM(st.settlement_amount), 0) as total_settlement_amount,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'COMPLETED'
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as completed_amount,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'IN_PROGRESS'
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as inprogress_amount
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND DATE(st.created_at) BETWEEN #{startDate} AND #{endDate}
        """)
    @ConstructorArgs({
            @Arg(column = "total_count", javaType = Long.class),
            @Arg(column = "total_settlement_amount", javaType = Long.class),
            @Arg(column = "completed_amount", javaType = Long.class),
            @Arg(column = "inprogress_amount", javaType = Long.class)
    })
    SettlementSummaryDTO getSettlementSummaryBySellerIdAndPeriod(
            @Param("sellerId") String sellerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 판매자의 이번달 정산현황 조회
     * 새로운 구조: 개수와 금액 모두 포함
     */
    @Select("""
        SELECT 
            COUNT(*) as total_count,
            COALESCE(SUM(st.settlement_amount), 0) as total_settlement_amount,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'COMPLETED'
                THEN 1
                ELSE 0 
            END), 0) as completed_count,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'COMPLETED'
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as completed_amount,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'IN_PROGRESS'
                THEN 1
                ELSE 0 
            END), 0) as inprogress_count,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'IN_PROGRESS'
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as inprogress_amount
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND EXTRACT(YEAR FROM st.created_at) = #{currentMonth.year}
        AND EXTRACT(MONTH FROM st.created_at) = #{currentMonth.monthValue}
        """)
    @ConstructorArgs({
            @Arg(column = "total_count", javaType = Long.class),
            @Arg(column = "total_settlement_amount", javaType = Long.class),
            @Arg(column = "completed_count", javaType = Long.class),
            @Arg(column = "completed_amount", javaType = Long.class),
            @Arg(column = "inprogress_count", javaType = Long.class),
            @Arg(column = "inprogress_amount", javaType = Long.class)
    })
    MonthlySettlementStatusDto getMonthlySettlementStatus(
            @Param("sellerId") String sellerId,
            @Param("currentMonth") YearMonth currentMonth
    );

    /**
     * 판매자의 월별 정산내역 조회 (영수증용 - 페이징)
     */
    @Select("""
        SELECT 
            o.order_number,
            p.title as product_name,
            st.item_price as order_amount,
            st.commission_amount as commission,
            st.settlement_amount,
            o.created_at as order_date,
            s.delivered_at as delivery_date,
            st.created_at as settlement_created_at,
            st.settlement_status
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
        LEFT JOIN shipments s ON o.id = s.order_id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND EXTRACT(YEAR FROM st.created_at) = #{targetMonth.year}
        AND EXTRACT(MONTH FROM st.created_at) = #{targetMonth.monthValue}
        ORDER BY st.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    @ConstructorArgs({
            @Arg(column = "order_number", javaType = String.class),
            @Arg(column = "product_name", javaType = String.class),
            @Arg(column = "order_amount", javaType = Long.class),
            @Arg(column = "commission", javaType = Long.class),
            @Arg(column = "settlement_amount", javaType = Long.class),
            @Arg(column = "order_date", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "delivery_date", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "settlement_created_at", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "settlement_status", javaType = com.team5.catdogeats.orders.domain.enums.SettlementStatus.class)
    })
    List<SettlementItemDTO> findMonthlySettlementsWithPaging(
            @Param("sellerId") String sellerId,
            @Param("targetMonth") YearMonth targetMonth,
            @Param("offset") long offset,
            @Param("limit") int limit
    );

    /**
     * 판매자의 월별 정산내역 조회 (CSV용 - 전체)
     */
    @Select("""
        SELECT 
            o.order_number,
            p.title as product_name,
            st.item_price as order_amount,
            st.commission_amount as commission,
            st.settlement_amount,
            o.created_at as order_date,
            s.delivered_at as delivery_date,
            st.created_at as settlement_created_at,
            st.settlement_status
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
        LEFT JOIN shipments s ON o.id = s.order_id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND EXTRACT(YEAR FROM st.created_at) = #{targetMonth.year}
        AND EXTRACT(MONTH FROM st.created_at) = #{targetMonth.monthValue}
        ORDER BY st.created_at DESC
        """)
    @ConstructorArgs({
            @Arg(column = "order_number", javaType = String.class),
            @Arg(column = "product_name", javaType = String.class),
            @Arg(column = "order_amount", javaType = Long.class),
            @Arg(column = "commission", javaType = Long.class),
            @Arg(column = "settlement_amount", javaType = Long.class),
            @Arg(column = "order_date", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "delivery_date", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "settlement_created_at", javaType = java.time.ZonedDateTime.class,
                    typeHandler = com.team5.catdogeats.global.config.mybatis.ZonedDateTimeTypeHandler.class),
            @Arg(column = "settlement_status", javaType = com.team5.catdogeats.orders.domain.enums.SettlementStatus.class)
    })
    List<SettlementItemDTO> findMonthlySettlements(
            @Param("sellerId") String sellerId,
            @Param("targetMonth") YearMonth targetMonth
    );

    /**
     * 판매자의 월별 정산 건수 조회 (페이징용)
     */
    @Select("""
        SELECT COUNT(*)
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND EXTRACT(YEAR FROM st.created_at) = #{targetMonth.year}
        AND EXTRACT(MONTH FROM st.created_at) = #{targetMonth.monthValue}
        """)
    Long countMonthlySettlements(
            @Param("sellerId") String sellerId,
            @Param("targetMonth") YearMonth targetMonth
    );

    /**
     * 판매자의 월별 정산 요약 정보 조회
     * 개수와 금액 모두 포함
     */
    @Select("""
        SELECT 
            COUNT(*) as total_count,
            COALESCE(SUM(st.settlement_amount), 0) as total_settlement_amount,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'COMPLETED'
                THEN 1
                ELSE 0 
            END), 0) as completed_count,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'COMPLETED'
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as completed_amount,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'IN_PROGRESS'
                THEN 1
                ELSE 0 
            END), 0) as inprogress_count,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'IN_PROGRESS'
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as inprogress_amount
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND EXTRACT(YEAR FROM st.created_at) = #{targetMonth.year}
        AND EXTRACT(MONTH FROM st.created_at) = #{targetMonth.monthValue}
        """)
    @ConstructorArgs({
            @Arg(column = "total_count", javaType = Long.class),
            @Arg(column = "total_settlement_amount", javaType = Long.class),
            @Arg(column = "completed_count", javaType = Long.class),
            @Arg(column = "completed_amount", javaType = Long.class),
            @Arg(column = "inprogress_count", javaType = Long.class),
            @Arg(column = "inprogress_amount", javaType = Long.class)
    })
    MonthlySettlementStatusDto getMonthlySettlementSummary(
            @Param("sellerId") String sellerId,
            @Param("targetMonth") YearMonth targetMonth
    );
}