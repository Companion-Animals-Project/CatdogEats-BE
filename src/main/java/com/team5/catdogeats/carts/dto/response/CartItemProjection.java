package com.team5.catdogeats.carts.dto.response;

public interface CartItemProjection {

    String getCartItemId();
    String getProductId();
    String getSellerId();
    Long getProductNumber();
    String getTitle();
    String getImageUrl();
    int getQuantity();
    Long getUnitPrice();
    Long getTotalPrice();
    Long getDeliveryFee();

}
