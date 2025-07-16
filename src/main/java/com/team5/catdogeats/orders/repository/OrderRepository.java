package com.team5.catdogeats.orders.repository;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.dto.common.GroupOrdersAndPayments;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 주문 엔티티 Repository
 * 기본 CRUD + 주문/결제/배송 조회 메서드를 제공합니다.
 */
public interface OrderRepository extends JpaRepository<Orders, String> {

    /**
     * 주문 상세 조회 (OrderItems·Products 포함)
     */
    @Query("""
        SELECT DISTINCT o
        FROM Orders o
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.products p
        WHERE o.buyers = :buyer
          AND o.orderNumber = :orderNumber
    """)
    Optional<Orders> findOrderDetailByUserAndOrderNumber(@Param("buyer") Buyers buyer, @Param("orderNumber") String orderNumber);

    /**
     * 구매자 주문 목록 조회 (페이징, OrderItems·Shipment 포함)
     */
    @Query("""
        SELECT DISTINCT o
        FROM Orders o
        LEFT JOIN FETCH o.orderItems oi
        LEFT JOIN FETCH oi.products p
        LEFT JOIN FETCH o.shipment s
        WHERE o.buyers = :buyer
          AND o.isHidden = false
    """)
    Page<Orders> findBuyerOrdersWithDetails(@Param("buyer") Buyers buyer, Pageable pageable);

    /**
     * 주문·결제 정보 DTO 투영 조회
     */
    @Query("""
        SELECT new com.team5.catdogeats.orders.dto.common.GroupOrdersAndPayments(o, p)
        FROM Payments p
        JOIN p.orders o
        WHERE o.id = :orderId
    """)
    Optional<GroupOrdersAndPayments> findGroupByOrdersAndPaymentsOrderId(@Param("orderId") String orderId);

}
