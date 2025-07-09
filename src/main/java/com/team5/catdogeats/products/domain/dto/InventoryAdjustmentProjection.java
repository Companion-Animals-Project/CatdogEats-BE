package com.team5.catdogeats.products.domain.dto;

import com.team5.catdogeats.products.domain.enums.StockStatus;

import java.time.ZonedDateTime;

public interface InventoryAdjustmentProjection {
    String getId();
    ZonedDateTime getUpdatedAt();

    String  getProductsTitle();
    Long    getProductsProductNumber();
    Integer getProductsStock();
    Integer getProductsSafetyStock();
    Long    getProductsPrice();
    Double  getProductsDiscountedPrice();
    Boolean getProductsDiscounted();

    default StockStatus getStockStatus() {
        if (getProductsStock() <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (getProductsStock() <= getProductsSafetyStock()) {
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.IN_STOCK;
    }
}
