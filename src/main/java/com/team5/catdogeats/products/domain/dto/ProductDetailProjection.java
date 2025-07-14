package com.team5.catdogeats.products.domain.dto;

import java.util.List;

public interface ProductDetailProjection {
    String getTitle();
    String getSubTitle();
    String getProductInfo();
    String getContents();
    Boolean getIsDiscounted();
    Double getDiscountRate();
    Long getPrice();
    List<String> getImages();
    String getVendorName();
    Double getAverageStar();
    Integer getReviewCount();
}
