package com.team5.catdogeats.products.domain.dto;

import com.team5.catdogeats.products.domain.enums.AdjustmentType;

import java.time.ZonedDateTime;

public interface InventoryAdjustmentProjection {
    String getId();
    ZonedDateTime getUpdatedAt();
    AdjustmentType getAdjustmentType();
    int getQuantity();
    String getNote();

    String  getProductsTitle();
    Long    getProductsProductNumber();

}
