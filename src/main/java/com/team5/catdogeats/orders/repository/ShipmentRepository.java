package com.team5.catdogeats.orders.repository;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Shipments 엔티티 Repository
 */
public interface ShipmentRepository extends JpaRepository<Shipments, String> {
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
}