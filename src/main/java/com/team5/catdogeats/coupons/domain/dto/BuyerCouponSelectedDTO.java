package com.team5.catdogeats.coupons.domain.dto;

import java.time.LocalDate;

public record BuyerCouponSelectedDTO(String id,
                                     String code,
                                     String couponName,
                                     String discountType,
                                     Integer discountValue,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     Boolean isUsed) {
}
