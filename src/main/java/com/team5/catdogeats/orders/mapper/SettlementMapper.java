package com.team5.catdogeats.orders.mapper;

import com.team5.catdogeats.orders.domain.dto.SettlementItemDto;
import com.team5.catdogeats.orders.domain.dto.SettlementSummaryDto;
import org.apache.ibatis.annotations.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 정산현황 조회를 위한 MyBatis Mapper (어노테이션 방식)
 * Settlements 테이블 기반으로 정산 데이터 조회
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
            o.created_at::timestamp as order_date,
            st.settlement_status
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
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
            @Arg(column = "order_date", javaType = java.time.LocalDateTime.class),
            @Arg(column = "settlement_status", javaType = com.team5.catdogeats.orders.domain.enums.SettlementStatus.class)
    })
    List<SettlementItemDto> findSettlementsBySellerId(
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
     * Record 클래스를 위한 생성자 기반 매핑
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
                WHEN st.settlement_status IN ('PENDING', 'IN_PROGRESS')
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as pending_amount,
            COALESCE(ROUND(AVG(st.settlement_amount)), 0) as average_settlement_amount
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
            @Arg(column = "pending_amount", javaType = Long.class),
            @Arg(column = "average_settlement_amount", javaType = Long.class)
    })
    SettlementSummaryDto getSettlementSummaryBySellerId(@Param("sellerId") String sellerId);

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
            o.created_at::timestamp as order_date,
            st.settlement_status
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND DATE(o.created_at) BETWEEN #{startDate} AND #{endDate}
        ORDER BY o.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """)
    @ConstructorArgs({
            @Arg(column = "order_number", javaType = String.class),
            @Arg(column = "product_name", javaType = String.class),
            @Arg(column = "order_amount", javaType = Long.class),
            @Arg(column = "commission", javaType = Long.class),
            @Arg(column = "settlement_amount", javaType = Long.class),
            @Arg(column = "order_date", javaType = java.time.LocalDateTime.class),
            @Arg(column = "settlement_status", javaType = com.team5.catdogeats.orders.domain.enums.SettlementStatus.class)
    })
    List<SettlementItemDto> findSettlementsBySellerIdAndPeriod(
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
        AND DATE(o.created_at) BETWEEN #{startDate} AND #{endDate}
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
                WHEN st.settlement_status IN ('PENDING', 'IN_PROGRESS')
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as pending_amount,
            COALESCE(ROUND(AVG(st.settlement_amount)), 0) as average_settlement_amount
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND DATE(o.created_at) BETWEEN #{startDate} AND #{endDate}
        """)
    @ConstructorArgs({
            @Arg(column = "total_count", javaType = Long.class),
            @Arg(column = "total_settlement_amount", javaType = Long.class),
            @Arg(column = "completed_amount", javaType = Long.class),
            @Arg(column = "pending_amount", javaType = Long.class),
            @Arg(column = "average_settlement_amount", javaType = Long.class)
    })
    SettlementSummaryDto getSettlementSummaryBySellerIdAndPeriod(
            @Param("sellerId") String sellerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 판매자의 이번달 정산현황 조회
     * PostgreSQL EXTRACT 함수 사용
     */
    @Select("""
        SELECT 
            COUNT(*) as total_count,
            COALESCE(SUM(st.settlement_amount), 0) as total_settlement_amount,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status IN ('COMPLETED', 'IN_PROGRESS')
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as completed_amount,
            COALESCE(SUM(CASE 
                WHEN st.settlement_status = 'PENDING'
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as pending_amount,
            COALESCE(ROUND(AVG(st.settlement_amount)), 0) as average_settlement_amount
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND EXTRACT(YEAR FROM o.created_at) = #{currentMonth.year}
        AND EXTRACT(MONTH FROM o.created_at) = #{currentMonth.monthValue}
        """)
    @ConstructorArgs({
            @Arg(column = "total_count", javaType = Long.class),
            @Arg(column = "total_settlement_amount", javaType = Long.class),
            @Arg(column = "completed_amount", javaType = Long.class),
            @Arg(column = "pending_amount", javaType = Long.class),
            @Arg(column = "average_settlement_amount", javaType = Long.class)
    })
    SettlementSummaryDto getMonthlySettlementStatus(
            @Param("sellerId") String sellerId,
            @Param("currentMonth") YearMonth currentMonth
    );

    /**
     * 판매자의 월별 정산내역 조회 (영수증용)
     * PostgreSQL EXTRACT 함수 사용
     */
    @Select("""
        SELECT 
            o.order_number,
            p.title as product_name,
            st.item_price as order_amount,
            st.commission_amount as commission,
            st.settlement_amount,
            o.created_at::timestamp as order_date,
            st.settlement_status
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND EXTRACT(YEAR FROM o.created_at) = #{targetMonth.year}
        AND EXTRACT(MONTH FROM o.created_at) = #{targetMonth.monthValue}
        ORDER BY o.created_at DESC
        """)
    @ConstructorArgs({
            @Arg(column = "order_number", javaType = String.class),
            @Arg(column = "product_name", javaType = String.class),
            @Arg(column = "order_amount", javaType = Long.class),
            @Arg(column = "commission", javaType = Long.class),
            @Arg(column = "settlement_amount", javaType = Long.class),
            @Arg(column = "order_date", javaType = java.time.LocalDateTime.class),
            @Arg(column = "settlement_status", javaType = com.team5.catdogeats.orders.domain.enums.SettlementStatus.class)
    })
    List<SettlementItemDto> findMonthlySettlements(
            @Param("sellerId") String sellerId,
            @Param("targetMonth") YearMonth targetMonth
    );

    /**
     * 판매자의 월별 정산 요약 정보 조회
     * PostgreSQL EXTRACT 함수 사용
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
                WHEN st.settlement_status IN ('PENDING', 'IN_PROGRESS')
                THEN st.settlement_amount
                ELSE 0 
            END), 0) as pending_amount,
            COALESCE(ROUND(AVG(st.settlement_amount)), 0) as average_settlement_amount
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        WHERE st.seller_id = #{sellerId}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND EXTRACT(YEAR FROM o.created_at) = #{targetMonth.year}
        AND EXTRACT(MONTH FROM o.created_at) = #{targetMonth.monthValue}
        """)
    @ConstructorArgs({
            @Arg(column = "total_count", javaType = Long.class),
            @Arg(column = "total_settlement_amount", javaType = Long.class),
            @Arg(column = "completed_amount", javaType = Long.class),
            @Arg(column = "pending_amount", javaType = Long.class),
            @Arg(column = "average_settlement_amount", javaType = Long.class)
    })
    SettlementSummaryDto getMonthlySettlementSummary(
            @Param("sellerId") String sellerId,
            @Param("targetMonth") YearMonth targetMonth
    );
}