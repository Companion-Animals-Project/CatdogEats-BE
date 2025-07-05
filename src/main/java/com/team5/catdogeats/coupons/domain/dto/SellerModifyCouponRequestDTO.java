package com.team5.catdogeats.coupons.domain.dto;

import com.team5.catdogeats.coupons.domain.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record SellerModifyCouponRequestDTO(@NotBlank String id,
                                           String couponName,
                                           String code,
                                           DiscountType discountType,
                                           Integer discountValue,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           Integer usageLimit) {
}
