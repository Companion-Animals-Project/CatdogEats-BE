package com.team5.catdogeats.products.domain.dto;

import java.time.Instant;

public interface ProductListProjection{
    String getProductId();
    String getImageUrl();
    String getProductName();
    String getVendorName();
    Double getAverageStar();
    Long getPrice();
    Double getDiscountRate();
    Boolean getIsDiscounted();
    String getPetCategory();
    String getProductCategory();
    String getStockStatus();
    Instant getCreatedAt();
}
