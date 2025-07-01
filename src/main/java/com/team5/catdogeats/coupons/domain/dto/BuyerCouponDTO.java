package com.team5.catdogeats.coupons.domain.dto;

import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.mapping.BuyerCoupons;
import com.team5.catdogeats.users.domain.mapping.Buyers;

import java.time.ZonedDateTime;

public record BuyerCouponDTO(Coupons coupons, Buyers buyers) {
    public static BuyerCoupons toEntity(BuyerCouponDTO dto) {
        return BuyerCoupons.builder()
                .coupons(dto.coupons())
                .buyers(dto.buyers())
                .isUsed(false)
                .usedAt(ZonedDateTime.now())
                .build();
    }
}
