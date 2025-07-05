package com.team5.catdogeats.orders.mapper;

import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;

/**
 * 정산 배치 처리를 위한 MyBatis Mapper
 * 정산 데이터 생성, 상태 갱신 등의 배치 작업을 담당
 */
@Mapper
public interface SettlementBatchMapper {

    /**
     * 배송완료된 주문아이템에 대해 정산 데이터 생성
     * @param commissionRate 수수료율
     * @return 생성된 정산 건수
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
            gen_random_uuid() as id,
            s.seller_id,
            oi.id as order_item_id,
            oi.price as item_price,
            #{commissionRate} as commission_rate,
            ROUND(oi.price * #{commissionRate}) as commission_amount,
            oi.price - ROUND(oi.price * #{commissionRate}) as settlement_amount,
            'PENDING' as settlement_status,
            NOW() as created_at,
            NOW() as updated_at
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN shipments s ON o.id = s.order_id
        LEFT JOIN settlements st ON oi.id = st.order_item_id
        WHERE s.delivered_at IS NOT NULL
          AND o.order_status = 'DELIVERED'
          AND o.is_hidden = false
          AND st.id IS NULL
        """)
    int createSettlementsForDeliveredItems(@Param("commissionRate") BigDecimal commissionRate);

    /**
     * 대기중 정산을 처리중으로 변경 (배송완료 후 7일 경과)
     * @return 업데이트된 정산 건수
     */
    @Update("""
        UPDATE settlements 
        SET settlement_status = 'IN_PROGRESS',
            updated_at = NOW()
        WHERE settlement_status = 'PENDING'
          AND id IN (
              SELECT st.id 
              FROM settlements st
              INNER JOIN order_items oi ON st.order_item_id = oi.id
              INNER JOIN orders o ON oi.order_id = o.id
              INNER JOIN shipments s ON o.id = s.order_id
              WHERE s.delivered_at IS NOT NULL
                AND s.delivered_at <= NOW() - INTERVAL '7 days'
                AND st.settlement_status = 'PENDING'
          )
        """)
    int updatePendingToInProgress();

    /**
     * 처리중 정산을 정산완료로 변경
     * @return 업데이트된 정산 건수
     */
    @Update("""
        UPDATE settlements 
        SET settlement_status = 'COMPLETED',
            settled_at = NOW(),
            updated_at = NOW()
        WHERE settlement_status = 'IN_PROGRESS'
        """)
    int updateInProgressToCompleted();

    /**
     * 정산 대상 주문아이템 건수 조회 (모니터링용)
     * @return 정산 대상 건수
     */
    @Select("""
        SELECT COUNT(*)
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN shipments s ON o.id = s.order_id
        LEFT JOIN settlements st ON oi.id = st.order_item_id
        WHERE s.delivered_at IS NOT NULL
          AND o.order_status = 'DELIVERED'
          AND o.is_hidden = false
          AND st.id IS NULL
        """)
    int countUnsettledItems();

    /**
     * 처리중 상태 정산 건수 조회 (모니터링용)
     * @return 처리중 정산 건수
     */
    @Select("""
        SELECT COUNT(*)
        FROM settlements
        WHERE settlement_status = 'IN_PROGRESS'
        """)
    int countInProgressSettlements();

    /**
     * 대기중 정산 중 7일 경과되어 처리중으로 변경 가능한 건수 조회 (모니터링용)
     * @return 처리중으로 변경 가능한 대기중 정산 건수
     */
    @Select("""
        SELECT COUNT(*)
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN shipments s ON o.id = s.order_id
        WHERE st.settlement_status = 'PENDING'
          AND s.delivered_at IS NOT NULL
          AND s.delivered_at <= NOW() - INTERVAL '7 days'
        """)
    int countPendingSettlementsReadyForProgress();

}