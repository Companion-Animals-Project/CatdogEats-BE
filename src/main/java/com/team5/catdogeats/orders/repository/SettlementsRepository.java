package com.team5.catdogeats.orders.repository;

import com.team5.catdogeats.orders.domain.Settlements;
import com.team5.catdogeats.orders.domain.enums.SettlementStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 정산 데이터 Repository
 */
@Repository
public interface SettlementsRepository extends JpaRepository<Settlements, String> {

    /**
     * 주문 상품으로 정산 데이터 조회
     * @param orderItems 주문 상품
     * @return 정산 데이터
     */
    Optional<Settlements> findByOrderItems(OrderItems orderItems);

    /**
     * 정산 상태로 정산 데이터 목록 조회
     * @param settlementStatus 정산 상태
     * @return 정산 데이터 목록
     */
    List<Settlements> findBySettlementStatus(SettlementStatus settlementStatus);

    /**
     * 판매자별 정산 상태로 정산 데이터 목록 조회
     * @param sellerId 판매자 ID
     * @param settlementStatus 정산 상태
     * @return 정산 데이터 목록
     */
    @Query("SELECT s FROM Settlements s WHERE s.seller.id = :sellerId AND s.settlementStatus = :settlementStatus")
    List<Settlements> findBySellerIdAndSettlementStatus(
            @Param("sellerId") String sellerId,
            @Param("settlementStatus") SettlementStatus settlementStatus);

    /**
     * 처리중 상태의 정산 데이터 중 매월 정산 대상 조회
     * (처리중으로 변경된지 일정 기간이 지난 데이터)
     * @return 정산 완료 대상 정산 데이터 목록
     */
    @Query("SELECT s FROM Settlements s WHERE s.settlementStatus = 'IN_PROGRESS'")
    List<Settlements> findInProgressSettlementsForCompletion();

    /**
     * 배송완료된 주문의 정산되지 않은 주문상품들 조회
     * @return 정산 데이터가 없는 주문상품들과 연관된 정산 대상 목록
     */
    @Query("""
        SELECT oi FROM OrderItems oi 
        JOIN oi.orders o 
        JOIN o.shipment s 
        WHERE o.orderStatus = 'DELIVERED' 
        AND o.isHidden = false 
        AND s.deliveredAt IS NOT NULL 
        AND NOT EXISTS (
            SELECT 1 FROM Settlements st 
            WHERE st.orderItems = oi
        )
    """)
    List<OrderItems> findUnsettledOrderItems();

    /**
     * 특정 기간 내 배송완료된 주문의 정산 대상 조회
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 정산 대상 주문상품 목록
     */
    @Query("""
        SELECT oi FROM OrderItems oi 
        JOIN oi.orders o 
        JOIN o.shipment s 
        WHERE o.orderStatus = 'DELIVERED' 
        AND o.isHidden = false 
        AND s.deliveredAt BETWEEN :startDate AND :endDate
    """)
    List<OrderItems> findOrderItemsDeliveredBetween(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate);

    /**
     * 정산 상태 변경이 필요한 정산 데이터 조회
     * (배송완료 7일 경과로 PENDING -> IN_PROGRESS 변경 대상)
     * @param sevenDaysAgo 7일 전 시점
     * @return 상태 변경 대상 정산 데이터 목록
     */
    @Query("""
        SELECT s FROM Settlements s 
        JOIN s.orderItems oi 
        JOIN oi.orders o 
        JOIN o.shipment sh 
        WHERE s.settlementStatus = 'PENDING' 
        AND sh.deliveredAt IS NOT NULL 
        AND sh.deliveredAt < :sevenDaysAgo
    """)
    List<Settlements> findPendingSettlementsForStatusUpdate(@Param("sevenDaysAgo") ZonedDateTime sevenDaysAgo);

    /**
     * 특정 판매자의 월별 정산 통계 조회
     * @param sellerId 판매자 ID
     * @param startOfMonth 월 시작일
     * @param endOfMonth 월 종료일
     * @return 월별 정산 통계
     */
    @Query("""
        SELECT 
            COUNT(s) as totalCount,
            COALESCE(SUM(s.settlementAmount), 0) as totalAmount,
            COALESCE(SUM(CASE WHEN s.settlementStatus = 'COMPLETED' THEN s.settlementAmount ELSE 0 END), 0) as completedAmount,
            COALESCE(SUM(CASE WHEN s.settlementStatus = 'IN_PROGRESS' THEN s.settlementAmount ELSE 0 END), 0) as inProgressAmount,
            COALESCE(SUM(CASE WHEN s.settlementStatus = 'PENDING' THEN s.settlementAmount ELSE 0 END), 0) as pendingAmount
        FROM Settlements s 
        JOIN s.orderItems oi 
        JOIN oi.orders o 
        WHERE s.seller.id = :sellerId 
        AND o.createdAt BETWEEN :startOfMonth AND :endOfMonth 
        AND o.orderStatus != 'CANCELLED' 
        AND o.isHidden = false
    """)
    Object[] getMonthlySettlementStatsBySellerId(
            @Param("sellerId") String sellerId,
            @Param("startOfMonth") ZonedDateTime startOfMonth,
            @Param("endOfMonth") ZonedDateTime endOfMonth);

    /**
     * 중복 정산 데이터 확인
     * @param orderItems 주문 상품
     * @return 중복 여부
     */
    boolean existsByOrderItems(OrderItems orderItems);
}
