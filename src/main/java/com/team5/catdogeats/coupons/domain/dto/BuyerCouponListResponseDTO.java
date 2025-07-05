package com.team5.catdogeats.coupons.domain.dto;

import java.util.List;

public record BuyerCouponListResponseDTO(BuyerCouponCountDTO count,
                                         List<BuyerCouponSelectedDTO> selected) {

}

