package com.team5.catdogeats.coupons.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.dto.SellerCouponListResponseDTO;

import java.util.List;

public interface SellerCouponListService {
    List<SellerCouponListResponseDTO> getSellerCoupons(UserPrincipal userPrincipal, int page, int size);
    List<SellerCouponListResponseDTO> getSellerCouponsWithVendorName(String vendorName, int page, int size);
}
