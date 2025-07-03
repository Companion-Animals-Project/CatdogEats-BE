package com.team5.catdogeats.products.domain.dto;

import java.time.Instant;

public interface MainProductProjection {
    String getProductId();
    String getImageUrl();
    String getVendorName();
    String getTitle();
    Double getAverageStar();
    Integer getReviewCount();
    Long getPrice();
    Boolean getIsDiscounted();
    Double getDiscountRate();
    Instant getCreatedAt();
}
