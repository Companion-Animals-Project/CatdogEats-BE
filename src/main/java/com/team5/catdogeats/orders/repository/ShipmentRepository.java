package com.team5.catdogeats.orders.repository;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Shipments 엔티티 Repository
 * 기존 메서드는 유지하고 판매자 기능에 필요한 메서드만 추가
 */
public interface ShipmentRepository extends JpaRepository<Shipments, String> {

    // ===== 기존 메서드들 (수정하지 않음) =====

    /**
     * 주문으로 배송 정보 조회
     */
    Optional<Shipments> findByOrders(Orders orders);

    /**
     * 주문 번호로 배송 정보 조회 (주문과 함께 조회)
     */
    @Query("""
    SELECT s FROM Shipments s
    JOIN FETCH s.orders o
    WHERE o.orderNumber = :orderNumber
    """)
    Optional<Shipments> findByOrderNumber(@Param("orderNumber") String orderNumber);

    /**
     * 운송장 번호로 배송 정보 조회
     */
    Optional<Shipments> findByTrackingNumber(String trackingNumber);

    /**
     * 주문 ID로 배송 정보 존재 여부 확인
     */
    boolean existsByOrders(Orders orders);

    // ===== 판매자 기능을 위한 추가 메서드들 =====

    /**
     * 판매자가 주문번호로 배송정보 조회 (권한 검증 포함)
     * 판매자는 본인이 판매한 상품이 포함된 주문의 배송지 정보만 조회할 수 있다
     */
    @Query("""
    SELECT DISTINCT s FROM Shipments s
    JOIN FETCH s.orders o
    JOIN FETCH o.orderItems oi
    JOIN FETCH oi.products p
    JOIN FETCH p.seller ps
    WHERE o.orderNumber = :orderNumber
    AND ps.userId = :sellerId
    """)
    Optional<Shipments> findShippingInfoByOrderNumberAndSeller(
            @Param("orderNumber") String orderNumber,
            @Param("sellerId") String sellerId
    );

    /**
     * 판매자의 주문 목록 조회 (페이징)
     * 판매자가 판매한 상품이 포함된 주문의 배송 정보 목록을 조회
     * 숨김 처리된 배송 정보는 제외
     */
    @Query("""
    SELECT DISTINCT s FROM Shipments s
    JOIN FETCH s.orders o
    JOIN o.orderItems oi
    JOIN oi.products p
    WHERE p.seller.userId = :sellerId
    AND (s.isHiddenBySeller = false OR s.isHiddenBySeller IS NULL)
    ORDER BY s.createdAt DESC
    """)
    Page<Shipments> findSellerOrdersWithPaging(
            @Param("sellerId") String sellerId,
            Pageable pageable
    );

    /**
     * 택배사와 운송장 번호 조합으로 배송 정보 조회 (중복 검증용)
     */
    Optional<Shipments> findByCourierAndTrackingNumber(String courier, String trackingNumber);

    /**
     * 판매자별 주문 상태별 개수 조회 (통계용)
     */
    @Query("""
    SELECT o.orderStatus, COUNT(DISTINCT o.id)
    FROM Shipments s
    JOIN s.orders o
    JOIN o.orderItems oi
    JOIN oi.products p
    WHERE p.seller.userId = :sellerId
    AND (s.isHiddenBySeller = false OR s.isHiddenBySeller IS NULL)
    GROUP BY o.orderStatus
    """)
    List<Object[]> findOrderStatusCountBySeller(@Param("sellerId") String sellerId);

    /**
     * 특정 기간 내 배송 완료된 주문 조회
     */
    @Query("""
    SELECT s FROM Shipments s
    WHERE s.deliveredAt BETWEEN :startDate AND :endDate
    ORDER BY s.deliveredAt DESC
    """)
    List<Shipments> findDeliveredBetween(
            @Param("startDate") ZonedDateTime startDate,
            @Param("endDate") ZonedDateTime endDate
    );

    /**
     * 배송중인 주문 목록 조회 (배치 작업용)
     */
    @Query("""
    SELECT s FROM Shipments s
    JOIN FETCH s.orders o
    WHERE o.orderStatus = :orderStatus
    AND s.trackingNumber IS NOT NULL
    AND s.courier IS NOT NULL
    AND s.shippedAt IS NOT NULL
    ORDER BY s.shippedAt ASC
    """)
    List<Shipments> findByOrderStatusAndTrackingNumberIsNotNull(
            @Param("orderStatus") OrderStatus orderStatus
    );



    // ===== 배치 작업용 메서드들 (DeliveryTrackingBatchService용) =====

    /**
     * 주문 상태별 배송 정보 개수 조회
     * @param orderStatus 주문 상태
     * @return 해당 상태의 배송 정보 개수
     */
    @Query("""
    SELECT COUNT(s)
    FROM Shipments s
    JOIN s.orders o
    WHERE o.orderStatus = :orderStatus
    """)
    long countByOrderStatus(@Param("orderStatus") OrderStatus orderStatus);

    /**
     * 오늘 배송 완료된 주문 개수 조회
     * @return 오늘 배송 완료된 주문 개수
     */
    @SuppressWarnings("SqlDialectInspection")
    @Query(value = """
    SELECT COUNT(*)
    FROM shipments s
    WHERE s.delivered_at::date = CURRENT_DATE
    """, nativeQuery = true)
    long countDeliveredToday();
}