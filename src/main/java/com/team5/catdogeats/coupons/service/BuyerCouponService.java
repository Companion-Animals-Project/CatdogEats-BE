package com.team5.catdogeats.coupons.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.dto.BuyerCreateCouponRequestDTO;

public interface BuyerCouponService {
    void createCoupon(UserPrincipal userPrincipal, BuyerCreateCouponRequestDTO dto);
}
