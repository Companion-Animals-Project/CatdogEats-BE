package com.team5.catdogeats.coupons.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.dto.SellerCreateCouponRequestDTO;

public interface SellerCouponService {
    void createCoupon(UserPrincipal userPrincipal,  SellerCreateCouponRequestDTO dto);
}
