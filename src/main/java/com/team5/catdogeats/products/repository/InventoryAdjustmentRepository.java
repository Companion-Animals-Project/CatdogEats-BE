package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.dto.InventoryAdjustmentProjection;
import com.team5.catdogeats.products.domain.mapping.InventoryAdjustments;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustments, String> {

    Page<InventoryAdjustmentProjection> findInventoryAdjustmentsByProducts_Seller(Sellers sellers, Pageable pageable);

}
