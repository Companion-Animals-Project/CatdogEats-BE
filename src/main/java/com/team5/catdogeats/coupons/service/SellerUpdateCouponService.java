package com.team5.catdogeats.coupons.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.dto.SellerDeleteCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.dto.SellerModifyCouponRequestDTO;

public interface SellerUpdateCouponService {
    void modifierCoupon(UserPrincipal userPrincipal, SellerModifyCouponRequestDTO dto);
    void deleteCoupon(UserPrincipal userPrincipal, SellerDeleteCouponRequestDTO dto);
}
