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
 * Shipments 엔티티 Repository (확장)
 * 배송 정보 및 배송지 정보 관리를 위한 데이터 접근 계층
 * 판매자 배송 관리 기능을 위한 메서드들이 추가됨
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
    Optional<Shipments> findByOrderNumber(@Param("orderNumber") String orderNumber);

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

    /**
     * 판매자가 주문번호로 배송정보 조회 (권한 검증 포함)
     * 판매자는 본인이 판매한 상품이 포함된 주문의 배송지 정보만 조회할 수 있다
     * OrderItems와 Products를 함께 조회하여 판매자 권한을 검증
     * @param orderNumber 주문 번호
     * @param sellerId 판매자 userId (Sellers의 userId 필드)
     * @return 배송 정보 (Optional)
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

    // ===== 판매자 배송 관리를 위한 새로운 메서드들 =====

    /**
     * 판매자별 주문 목록 조회 (페이징)
     * 판매자가 본인이 판매한 상품이 포함된 주문들을 페이징으로 조회
     * 숨김 처리되지 않은 주문만 조회 (is_hidden_by_seller = false)
     * @param sellerId 판매자 userId
     * @param pageable 페이징 정보
     * @return 주문 목록 (페이징)
     */
    @Query("""
    SELECT DISTINCT s FROM Shipments s
    JOIN FETCH s.orders o
    JOIN FETCH o.orderItems oi
    JOIN FETCH oi.products p
    JOIN FETCH p.seller ps
    WHERE ps.userId = :sellerId
    AND (s.isHiddenBySeller IS NULL OR s.isHiddenBySeller = false)
    ORDER BY o.createdAt DESC
    """)
    Page<Shipments> findSellerOrdersWithPaging(
            @Param("sellerId") String sellerId,
            Pageable pageable
    );

    /**
     * 판매자별 주문 목록 조회 - 상태 필터링 (페이징)
     * 특정 주문 상태에 해당하는 주문들만 조회
     * @param sellerId 판매자 userId
     * @param orderStatus 주문 상태
     * @param pageable 페이징 정보
     * @return 주문 목록 (페이징)
     */
    @Query("""
    SELECT DISTINCT s FROM Shipments s
    JOIN FETCH s.orders o
    JOIN FETCH o.orderItems oi
    JOIN FETCH oi.products p
    JOIN FETCH p.seller ps
    WHERE ps.userId = :sellerId
    AND o.orderStatus = :orderStatus
    AND (s.isHiddenBySeller IS NULL OR s.isHiddenBySeller = false)
    ORDER BY o.createdAt DESC
    """)
    Page<Shipments> findSellerOrdersByStatusWithPaging(
            @Param("sellerId") String sellerId,
            @Param("orderStatus") OrderStatus orderStatus,
            Pageable pageable
    );

    /**
     * 판매자별 주문 검색 - 주문번호로 검색 (페이징)
     * @param sellerId 판매자 userId
     * @param orderNumber 검색할 주문번호 (부분 일치)
     * @param pageable 페이징 정보
     * @return 주문 목록 (페이징)
     */
    @Query("""
    SELECT DISTINCT s FROM Shipments s
    JOIN FETCH s.orders o
    JOIN FETCH o.orderItems oi
    JOIN FETCH oi.products p
    JOIN FETCH p.seller ps
    WHERE ps.userId = :sellerId
    AND o.orderNumber LIKE %:orderNumber%
    AND (s.isHiddenBySeller IS NULL OR s.isHiddenBySeller = false)
    ORDER BY o.createdAt DESC
    """)
    Page<Shipments> findSellerOrdersByOrderNumberWithPaging(
            @Param("sellerId") String sellerId,
            @Param("orderNumber") String orderNumber,
            Pageable pageable
    );

    /**
     * 판매자별 주문 검색 - 수령인명으로 검색 (페이징)
     * @param sellerId 판매자 userId
     * @param recipientName 검색할 수령인명 (부분 일치)
     * @param pageable 페이징 정보
     * @return 주문 목록 (페이징)
     */
    @Query("""
    SELECT DISTINCT s FROM Shipments s
    JOIN FETCH s.orders o
    JOIN FETCH o.orderItems oi
    JOIN FETCH oi.products p
    JOIN FETCH p.seller ps
    WHERE ps.userId = :sellerId
    AND s.recipientName LIKE %:recipientName%
    AND (s.isHiddenBySeller IS NULL OR s.isHiddenBySeller = false)
    ORDER BY o.createdAt DESC
    """)
    Page<Shipments> findSellerOrdersByRecipientNameWithPaging(
            @Param("sellerId") String sellerId,
            @Param("recipientName") String recipientName,
            Pageable pageable
    );

    /**
     * 운송장 번호 중복 확인
     * 동일한 택배사에서 같은 운송장 번호가 이미 등록되어 있는지 확인
     * @param courier 택배사
     * @param trackingNumber 운송장 번호
     * @return 중복 여부
     */
    boolean existsByCourierAndTrackingNumber(String courier, String trackingNumber);

    /**
     * 배송중인 주문 목록 조회 (배치 작업용)
     * 배송 추적 API 호출을 위해 배송중 상태인 모든 주문을 조회
     * @return 배송중인 배송 정보 목록
     */
    @Query("""
    SELECT s FROM Shipments s
    JOIN FETCH s.orders o
    WHERE o.orderStatus = :orderStatus
    AND s.trackingNumber IS NOT NULL
    AND s.courier IS NOT NULL
    """)
    List<Shipments> findAllInDeliveryOrders(@Param("orderStatus") OrderStatus orderStatus);

    /**
     * 판매자의 전체 주문 수 조회
     * 통계 정보 제공을 위한 메서드
     * @param sellerId 판매자 userId
     * @return 전체 주문 수
     */
    @Query("""
    SELECT COUNT(DISTINCT o.id) FROM Shipments s
    JOIN s.orders o
    JOIN o.orderItems oi
    JOIN oi.products p
    JOIN p.seller ps
    WHERE ps.userId = :sellerId
    AND (s.isHiddenBySeller IS NULL OR s.isHiddenBySeller = false)
    """)
    Long countSellerTotalOrders(@Param("sellerId") String sellerId);

    /**
     * 판매자의 상태별 주문 수 조회
     * @param sellerId 판매자 userId
     * @param orderStatus 주문 상태
     * @return 해당 상태의 주문 수
     */
    @Query("""
    SELECT COUNT(DISTINCT o.id) FROM Shipments s
    JOIN s.orders o
    JOIN o.orderItems oi
    JOIN oi.products p
    JOIN p.seller ps
    WHERE ps.userId = :sellerId
    AND o.orderStatus = :orderStatus
    AND (s.isHiddenBySeller IS NULL OR s.isHiddenBySeller = false)
    """)
    Long countSellerOrdersByStatus(
            @Param("sellerId") String sellerId,
            @Param("orderStatus") OrderStatus orderStatus
    );

    /**
     * 특정 기간 내 배송 완료된 주문 목록 조회
     * 정산 처리 등을 위한 메서드
     * @param sellerId 판매자 userId
     * @param startDate 시작일 (yyyy-MM-dd 형식)
     * @param endDate 종료일 (yyyy-MM-dd 형식)
     * @return 배송 완료된 주문 목록
     */
    @Query("""
    SELECT s FROM Shipments s
    JOIN FETCH s.orders o
    JOIN FETCH o.orderItems oi
    JOIN FETCH oi.products p
    JOIN FETCH p.seller ps
    WHERE ps.userId = :sellerId
    AND o.orderStatus = 'DELIVERED'
    AND DATE(s.deliveredAt) BETWEEN :startDate AND :endDate
    ORDER BY s.deliveredAt DESC
    """)
    List<Shipments> findSellerDeliveredOrdersByDateRange(
            @Param("sellerId") String sellerId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
}