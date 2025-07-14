package com.team5.catdogeats.orders.dto;

import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.users.domain.mapping.Sellers;

public record GroupSellerAndCouponsDTO(Coupons coupons, Sellers sellers) {
}
