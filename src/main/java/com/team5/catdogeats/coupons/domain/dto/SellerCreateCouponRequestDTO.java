package com.team5.catdogeats.coupons.domain.dto;

import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.enums.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record SellerCreateCouponRequestDTO(@NotBlank String couponName,
                                           @NotBlank String code,
                                           @NotNull DiscountType discountType,
                                           @NotNull Integer discountValue,
                                           @NotNull LocalDate startDate,
                                           @NotNull LocalDate endDate,
                                           Integer usageLimit) {
    public static Coupons buildDTO(SellerCreateCouponRequestDTO dto) {
        return Coupons.builder()
                .couponName(dto.couponName())
                .code(dto.code())
                .discountType(dto.discountType())
                .discountValue(dto.discountValue())
                .startDate(dto.startDate())
                .endDate(dto.endDate())
                .usageLimit(dto.usageLimit())
                .build();
    }
}
