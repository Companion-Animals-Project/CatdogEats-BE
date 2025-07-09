package com.team5.catdogeats.products.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.products.domain.dto.AdjustmentRequestDTO;
import com.team5.catdogeats.products.domain.dto.InventoryAdjustmentProjection;
import org.springframework.data.domain.Page;

public interface InventoryAdjustmentService {
    void updateAdjustment(UserPrincipal userPrincipal, AdjustmentRequestDTO dto);
    Page<InventoryAdjustmentProjection> adjustment(UserPrincipal userPrincipal, int page, int size);
}
