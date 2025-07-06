package com.team5.catdogeats.orders.mapper;

import com.team5.catdogeats.orders.domain.dto.SettlementBatchItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 정산 청크 배치 처리를 위한 MyBatis Mapper
 * ItemReader에서 사용할 페이징 쿼리들
 */
@Mapper
public interface SettlementChunkMapper {

    /**
     * 정산 생성 대상 주문아이템 조회 (페이징)
     * 배송완료되었지만 정산이 생성되지 않은 주문아이템들
     */
    @Select("""
        SELECT 
            s.seller_id,
            oi.id as order_item_id,
            o.order_number,
            p.title as product_title,
            oi.price as item_price
        FROM order_items oi
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
        INNER JOIN shipments s ON o.id = s.order_id
        LEFT JOIN settlements st ON oi.id = st.order_item_id
        WHERE s.delivered_at IS NOT NULL
          AND o.order_status = 'DELIVERED'
          AND o.is_hidden = false
          AND st.id IS NULL
        ORDER BY s.delivered_at ASC, oi.id ASC
        LIMIT #{limit} OFFSET #{offset}
        """)
    @Results({
            @Result(property = "sellerId", column = "seller_id"),
            @Result(property = "orderItemId", column = "order_item_id"),
            @Result(property = "orderNumber", column = "order_number"),
            @Result(property = "productTitle", column = "product_title"),
            @Result(property = "itemPrice", column = "item_price")
    })
    List<SettlementBatchItem> findUnsettledItems(@Param("offset") int offset,
                                                 @Param("limit") int limit);

    /**
     * 정산 상태 갱신 대상 조회 (페이징)
     * 배송완료 후 7일 경과한 PENDING 정산들
     */
    @Select("""
        SELECT 
            st.id as settlement_id,
            st.seller_id,
            o.order_number,
            p.title as product_title,
            s.delivered_at
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
        INNER JOIN shipments s ON o.id = s.order_id
        WHERE st.settlement_status = 'PENDING'
          AND s.delivered_at IS NOT NULL
          AND s.delivered_at <= NOW() - INTERVAL '7 days'
        ORDER BY s.delivered_at ASC, st.id ASC
        LIMIT #{limit} OFFSET #{offset}
        """)
    @Results({
            @Result(property = "settlementId", column = "settlement_id"),
            @Result(property = "sellerId", column = "seller_id"),
            @Result(property = "orderNumber", column = "order_number"),
            @Result(property = "productTitle", column = "product_title"),
            @Result(property = "deliveredAt", column = "delivered_at")
    })
    List<SettlementBatchItem> findPendingSettlementsReadyForProgress(@Param("offset") int offset,
                                                                     @Param("limit") int limit);

    /**
     * 정산 완료 대상 조회 (페이징)
     * IN_PROGRESS 상태의 모든 정산들
     */
    @Select("""
        SELECT 
            st.id as settlement_id,
            st.seller_id,
            o.order_number,
            p.title as product_title,
            st.settlement_amount
        FROM settlements st
        INNER JOIN order_items oi ON st.order_item_id = oi.id
        INNER JOIN orders o ON oi.order_id = o.id
        INNER JOIN products p ON oi.product_id = p.id
        WHERE st.settlement_status = 'IN_PROGRESS'
        ORDER BY st.created_at ASC, st.id ASC
        LIMIT #{limit} OFFSET #{offset}
        """)
    @Results({
            @Result(property = "settlementId", column = "settlement_id"),
            @Result(property = "sellerId", column = "seller_id"),
            @Result(property = "orderNumber", column = "order_number"),
            @Result(property = "productTitle", column = "product_title"),
            @Result(property = "settlementAmount", column = "settlement_amount")
    })
    List<SettlementBatchItem> findInProgressSettlements(@Param("offset") int offset,
                                                        @Param("limit") int limit);

    /**
     * 정산 데이터 생성 (단건)
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
        ) VALUES (
            gen_random_uuid(),
            #{sellerId},
            #{orderItemId},
            #{itemPrice},
            #{commissionRate},
            #{commissionAmount},
            #{settlementAmount},
            'PENDING',
            NOW(),
            NOW()
        )
        """)
    void insertSettlement(SettlementBatchItem item);

    /**
     * 정산 상태를 IN_PROGRESS로 변경 (단건)
     */
    @Update("""
        UPDATE settlements 
        SET settlement_status = 'IN_PROGRESS',
            updated_at = NOW()
        WHERE id = #{settlementId}
          AND settlement_status = 'PENDING'
        """)
    int updateToInProgress(@Param("settlementId") String settlementId);

    /**
     * 정산 상태를 COMPLETED로 변경 (단건)
     */
    @Update("""
        UPDATE settlements 
        SET settlement_status = 'COMPLETED',
            settled_at = NOW(),
            updated_at = NOW()
        WHERE id = #{settlementId}
          AND settlement_status = 'IN_PROGRESS'
        """)
    int updateToCompleted(@Param("settlementId") String settlementId);



    /**
     * 정산 생성 대상 전체 건수
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
     * 정산 상태 갱신 대상 전체 건수
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

    /**
     * 정산 완료 대상 전체 건수
     */
    @Select("""
        SELECT COUNT(*)
        FROM settlements
        WHERE settlement_status = 'IN_PROGRESS'
        """)
    int countInProgressSettlements();
}