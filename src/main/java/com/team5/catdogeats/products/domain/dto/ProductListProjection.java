package com.team5.catdogeats.products.domain.dto;

import java.time.Instant;

public interface ProductListProjection{
    String getProductId();
    String getProductNumber();
    Integer getStock();
    String getImageUrl();
    String getProductName();
    String getVendorName();
    Double getAverageStar();
    Long getPrice();
    Double getDiscountRate();
    Boolean getIsDiscounted();
    Long getDiscountedPrice();
    String getPetCategory();
    String getProductCategory();
    String get();
    Instant getCreatedAt();
    Integer getReviewCount();
}
