package com.team5.catdogeats.orders.repository;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.products.domain.dto.ProductDeliveredResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 주문 상품 Repository
// 결제 완료 시 구매한 상품 정보 조회를 위해 사용
public interface OrderItemRepository extends JpaRepository<OrderItems, String> {

    // 특정 주문의 주문 상품들 조회
    // 결제 완료 시 구매한 상품 ID 목록을 가져오기 위해 사용
    List<OrderItems> findByOrdersId(String orderId);

    // 배송 완료된 상품인지 검증하는 용도
    boolean existsByOrders_Buyers_User_IdAndProducts_IdAndOrders_OrderStatus(
            String userId, String productId, OrderStatus orderStatus
    );

    @Query(
            value = """
        SELECT 
            oi.product_id AS productId,
            (
                SELECT img.image_url
                FROM products_images pi
                JOIN images img ON pi.product_image_id = img.id
                WHERE pi.product_id = oi.product_id
                ORDER BY pi.created_at ASC
                LIMIT 1
            ) AS productImage,
            p.title AS productName,
            o.updated_at AS deliveredAt
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        JOIN products p ON oi.product_id = p.id
        WHERE o.buyer_id = :userId
          AND o.order_status = 'DELIVERED'
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        WHERE o.buyer_id = :userId
          AND o.order_status = 'DELIVERED'
        """,
            nativeQuery = true
    )
    Page<Object[]> findDeliveredProductsByUserId(@Param("userId") String userId, Pageable pageable);

}