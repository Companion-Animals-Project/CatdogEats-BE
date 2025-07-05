package com.team5.catdogeats.coupons.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record BuyerCreateCouponRequestDTO(@NotBlank String code) {
}
