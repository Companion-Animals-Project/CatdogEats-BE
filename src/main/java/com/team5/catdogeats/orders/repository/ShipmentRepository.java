package com.team5.catdogeats.orders.repository;

import com.team5.catdogeats.orders.domain.Shipments;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Shipments 엔티티 Repository
 * 배송 정보 및 배송지 정보 관리를 위한 데이터 접근 계층
 */
public interface ShipmentRepository extends JpaRepository<Shipments, String> {

}