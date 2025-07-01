package com.team5.catdogeats.coupons.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.dto.BuyerCouponListResponseDTO;
import com.team5.catdogeats.coupons.domain.dto.BuyerCreateCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.enums.CouponFilterType;

public interface BuyerCouponService {
    void createCoupon(UserPrincipal userPrincipal, BuyerCreateCouponRequestDTO dto);
    BuyerCouponListResponseDTO getBuyerCoupons(UserPrincipal userPrincipal, CouponFilterType filter, int page, int size);
}
