package com.team5.catdogeats.orders.repository;

import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// 주문 상품 Repository
// 결제 완료 시 구매한 상품 정보 조회를 위해 사용
public interface OrderItemRepository extends JpaRepository<OrderItems, String> {

    // 특정 주문의 주문 상품들 조회
    // 결제 완료 시 구매한 상품 ID 목록을 가져오기 위해 사용
    List<OrderItems> findByOrdersId(String orderId);
}