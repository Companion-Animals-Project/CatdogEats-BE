package com.team5.catdogeats.coupons.domain.dto;

import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.mapping.SellerCoupons;
import com.team5.catdogeats.users.domain.mapping.Sellers;

public record SellerCouponDTO(Coupons coupons, Sellers sellers) {

    public static SellerCoupons toEntity(SellerCouponDTO dto) {
        return SellerCoupons.builder()
                .coupons(dto.coupons())
                .sellers(dto.sellers())
                .build();
    }
}
