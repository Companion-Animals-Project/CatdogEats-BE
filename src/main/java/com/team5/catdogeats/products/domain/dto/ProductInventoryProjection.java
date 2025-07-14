package com.team5.catdogeats.products.domain.dto;

import com.team5.catdogeats.products.domain.enums.StockStatus;

public interface ProductInventoryProjection {
    String getId();
    String getTitle();
    Long getProductNumber();
    Integer getStock();
    Integer getSafetyStock();
    Long getPrice();
    Long getDiscountedPrice();
    Boolean getDiscounted();

    default StockStatus getStockStatus() {
        if (getStock() <= 0) return StockStatus.OUT_OF_STOCK;
        if (getStock() <= getSafetyStock()) return StockStatus.LOW_STOCK;
        return StockStatus.IN_STOCK;
    }
}
