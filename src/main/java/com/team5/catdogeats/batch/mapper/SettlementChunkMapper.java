package com.team5.catdogeats.batch.mapper;

import org.apache.ibatis.annotations.*;

/**
 * 정산 배치용 MyBatis Mapper (정리된 버전)
 * Admin Controller에서 현황 조회용으로만 사용
 */
@Mapper
public interface SettlementChunkMapper {

    /**
     * 정산 생성 대상 전체 건수 (Admin Controller용)
     */
    @Select("""
        SELECT COUNT(*)
        FROM shipments s
        INNER JOIN orders o ON s.order_id = o.id
        INNER JOIN order_items oi ON o.id = oi.order_id
        WHERE s.delivered_at IS NOT NULL
          AND s.delivered_at <= CURRENT_DATE - INTERVAL '7 days'
          AND s.delivered_at >= CURRENT_DATE - INTERVAL '14 days'
          AND o.order_status = 'DELIVERED'
          AND o.is_hidden = false
          AND NOT EXISTS (
              SELECT 1 FROM settlements st 
              WHERE st.order_item_id = oi.id
          )
        """)
    int countUnsettledItems();

    /**
     * 정산 완료 대상 전체 건수 (Admin Controller용)
     */
    @Select("""
        SELECT COUNT(*)
        FROM settlements
        WHERE settlement_status = 'IN_PROGRESS'
          AND created_at >= CURRENT_DATE - INTERVAL '60 days'
        """)
    int countInProgressSettlements();
}