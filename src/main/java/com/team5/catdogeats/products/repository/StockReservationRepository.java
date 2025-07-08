package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 재고 예약 Repository (타입 수정됨)
// 재고 예약 시스템의 데이터 접근 계층입니다.
// Products와 Orders 엔티티의 ID 타입이 String으로 변경됨에 따라 관련 메서드들을 수정하였습니다.
public interface StockReservationRepository extends JpaRepository<StockReservation, String> {

    // === 기본 조회 메서드 (타입 수정) ===

    // 주문 ID로 재고 예약 목록 조회 (타입 수정: UUID → String)
    List<StockReservation> findByOrderId(String orderId);


    // === 재고 수량 계산 메서드 (타입 수정) ===

    // 특정 상품의 총 예약 수량 조회 (타입 수정: UUID → String)
    @Query("SELECT COALESCE(SUM(sr.reservedQuantity), 0) FROM StockReservation sr " +
            "WHERE sr.product.id = :productId AND sr.reservationStatus = 'RESERVED'")
    Integer getTotalReservedQuantity(@Param("productId") String productId);

    void deleteByOrderId(String orderId);
}