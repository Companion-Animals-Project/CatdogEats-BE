package com.team5.catdogeats.products.domain.dto;

public interface ProductDetailProjection {
    String getTitle();
    String getSubTitle();
    String getProductInfo();
    String getContents();
    Boolean getIsDiscounted();
    Double getDiscountRate();
    Long getPrice();
    String getImages();          // JSON array string
    String getVendorName();
    Double getAverageStar();
    Integer getReviewCount();
}
