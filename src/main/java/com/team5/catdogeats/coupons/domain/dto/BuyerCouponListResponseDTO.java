package com.team5.catdogeats.coupons.domain.dto;

import java.time.LocalDate;

public record BuyerCouponListResponseDTO(String id,
                                         String code,
                                         String couponName,
                                         String discountType,
                                         Integer discountValue,
                                         LocalDate startDate,
                                         LocalDate endDate,
                                         Boolean isUsed) {
}
