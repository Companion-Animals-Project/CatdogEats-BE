package com.team5.catdogeats.orders.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * 정산 데이터 업데이트를 위한 MyBatis Mapper (어노테이션 방식)
 * 스케줄러에서 사용하는 정산 갱신 쿼리들
 */
@Mapper
public interface SettlementUpdateMapper {

    /**
     * 어제 주문의 정산 데이터 생성
     */
    @Insert("""
        INSERT INTO settlements (
            id,
            seller_id,
            order_item_id,
            item_price,
            commission_rate,
            commission_amount,
            settlement_amount,
            settlement_status,
            created_at,
            updated_at
        )
        SELECT 
            UUID() as id,
            p.seller_id,
            oi.id as order_item_id,
            (oi.price * oi.quantity) as item_price,
            10.00 as commission_rate,
            ROUND((oi.price * oi.quantity) * 0.1) as commission_amount,
            (oi.price * oi.quantity) - ROUND((oi.price * oi.quantity) * 0.1) as settlement_amount,
            'PENDING' as settlement_status,
            NOW() as created_at,
            NOW() as updated_at
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
        WHERE DATE(o.created_at) = #{targetDate}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND NOT EXISTS (
            SELECT 1 FROM settlements st 
            WHERE st.order_item_id = oi.id
        )
        """)
    long insertDailySettlements(@Param("targetDate") LocalDate targetDate);

    /**
     * 배송 상태에 따른 정산 상태 갱신
     * 배송완료 후 7일 경과 시 대기중 → 처리중으로 변경
     */
    @Update("""
        UPDATE settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        LEFT JOIN shipments s ON o.id = s.order_id
        SET 
            st.settlement_status = CASE 
                WHEN s.delivered_at IS NOT NULL 
                     AND s.delivered_at <= CURDATE() - INTERVAL 7 DAY
                     AND YEAR(o.created_at) = YEAR(CURDATE()) 
                     AND MONTH(o.created_at) = MONTH(CURDATE())
                THEN 'IN_PROGRESS'
                
                WHEN s.delivered_at IS NOT NULL 
                     AND s.delivered_at > CURDATE() - INTERVAL 7 DAY
                THEN 'PENDING'
                
                WHEN (YEAR(o.created_at) < YEAR(CURDATE())) 
                     OR (YEAR(o.created_at) = YEAR(CURDATE()) AND MONTH(o.created_at) < MONTH(CURDATE()))
                THEN 'COMPLETED'
                
                ELSE 'PENDING'
            END,
            st.updated_at = NOW()
        WHERE o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        AND st.settlement_status != 'COMPLETED'
        """)
    long updateSettlementStatuses();

    /**
     * 지난달 정산 완료 처리
     * 지난달의 처리중 상태를 정산완료로 변경
     */
    @Update("""
        UPDATE settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        SET 
            st.settlement_status = 'COMPLETED',
            st.settled_at = NOW(),
            st.updated_at = NOW()
        WHERE st.settlement_status = 'IN_PROGRESS'
        AND YEAR(o.created_at) = #{lastMonth.year}
        AND MONTH(o.created_at) = #{lastMonth.monthValue}
        AND o.order_status != 'CANCELLED'
        AND o.is_hidden = false
        """)
    long completeLastMonthSettlements(@Param("lastMonth") YearMonth lastMonth);
}
