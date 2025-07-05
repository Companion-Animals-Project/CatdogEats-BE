package com.team5.catdogeats.orders.repository;

import com.team5.catdogeats.orders.domain.OrderPendingDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * OrderPendingDetails 엔티티 Repository
 * 결제 대기 중인 주문의 상세 정보 관리를 위한 데이터 접근 계층
 */
public interface OrderPendingDetailsRepository extends JpaRepository<OrderPendingDetails, String> {

    /**
     * 주문 ID로 대기 정보 조회
     * @param orderId 주문 ID
     * @return 주문 대기 정보 (Optional)
     */
    Optional<OrderPendingDetails> findByOrderId(String orderId);

    /**
     * 주문 ID로 대기 정보 삭제
     * @param orderId 주문 ID
     */
    void deleteByOrderId(String orderId);

    /**
     * 주문 ID로 대기 정보 존재 여부 확인
     * @param orderId 주문 ID
     * @return 존재 여부
     */
    boolean existsByOrderId(String orderId);
}