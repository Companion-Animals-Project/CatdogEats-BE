// SellerProductListProjection.java
package com.team5.catdogeats.products.domain.dto;

import java.time.Instant;

public interface SellerProductListProjection {
    String getImageUrl();
    String getTitle();
    String getProductId();
    Long getProductNumber();
    String getPetCategory();
    String getProductCategory();
    Long getPrice();
    Integer getStock();
    Instant getUpdatedAt();
}
