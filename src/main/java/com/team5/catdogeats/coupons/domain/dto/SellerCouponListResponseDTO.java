package com.team5.catdogeats.coupons.domain.dto;

import com.team5.catdogeats.coupons.domain.enums.DiscountType;

import java.time.LocalDate;

public record SellerCouponListResponseDTO(String id,
                                          String code,
                                          String couponName,
                                          DiscountType discountType,
                                          Integer discountValue,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          Integer usageLimit) {
}
