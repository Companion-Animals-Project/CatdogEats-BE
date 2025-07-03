package com.team5.catdogeats.orders.repository;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Shipments 엔티티 Repository
 * 배송 정보 및 배송지 정보 관리를 위한 데이터 접근 계층
 */
public interface ShipmentRepository extends JpaRepository<Shipments, String> {

    /**
     * 주문으로 배송 정보 조회
     * @param orders 주문 엔티티
     * @return 배송 정보 (Optional)
     */
    Optional<Shipments> findByOrders(Orders orders);

    /**
     * 주문 번호로 배송 정보 조회 (주문과 함께 조회)
     * @param orderNumber 주문 번호
     * @return 배송 정보 (Optional)
     */
    @Query("""
    SELECT s FROM Shipments s
    JOIN FETCH s.orders o
    WHERE o.orderNumber = :orderNumber
    """)
    Optional<Shipments> findByOrderNumber(@Param("orderNumber") Long orderNumber);

    /**
     * 운송장 번호로 배송 정보 조회
     * @param trackingNumber 운송장 번호
     * @return 배송 정보 (Optional)
     */
    Optional<Shipments> findByTrackingNumber(String trackingNumber);

    /**
     * 주문 ID로 배송 정보 존재 여부 확인
     * @param orders 주문 엔티티
     * @return 존재 여부
     */
    boolean existsByOrders(Orders orders);
}