package com.team5.catdogeats.coupons.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.dto.SellerDeleteCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.dto.SellerModifyCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.enums.DiscountType;
import com.team5.catdogeats.coupons.mapper.CouponMapper;
import com.team5.catdogeats.coupons.repository.CouponRepository;
import com.team5.catdogeats.coupons.repository.SellerCouponRepository;
import com.team5.catdogeats.coupons.service.SellerUpdateCouponService;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerUpdateCouponServiceImpl implements SellerUpdateCouponService {
    private final CouponMapper couponMapper;
    private final CouponRepository couponRepository;
    private final SellerCouponRepository sellerCouponRepository;

    @Override
    @MybatisTransactional
    public void modifierCoupon(UserPrincipal userPrincipal,  SellerModifyCouponRequestDTO dto) {

        try {
            Coupons coupon = couponMapper.findCouponByIdAndProviderAndProviderId(dto.id(), userPrincipal.provider(), userPrincipal.providerId())
                    .orElseThrow(() -> new NoSuchElementException("쿠폰 정보를 찾을 수 없습니다."));
            validate(coupon, dto);
            couponMapper.updateCoupons(dto.id(),
                                        dto.couponName(),
                                        dto.code(),
                                        dto.discountType(),
                                        dto.discountValue(),
                                        dto.startDate(),
                                        dto.endDate(),
                                        dto.usageLimit());
        } catch (Exception e) {
            log.error("Error updating coupon: {}", e.getMessage());
            throw new RuntimeException("Error updating coupon: " + e.getMessage());
        }

    }

    @Override
    @JpaTransactional
    public void deleteCoupon(UserPrincipal userPrincipal, SellerDeleteCouponRequestDTO dto) {
        try {
            Coupons coupon = couponMapper.findCouponByIdAndProviderAndProviderId(dto.id(), userPrincipal.provider(), userPrincipal.providerId())
                    .orElseThrow(() -> new NoSuchElementException("쿠폰 정보를 찾을 수 없습니다."));
            sellerCouponRepository.deleteByCoupons(coupon);
            couponRepository.deleteCoupon(coupon);
        } catch (Exception e) {
            log.error("Error deleting coupon: {}", e.getMessage());
            throw new RuntimeException("Error deleting coupon: " + e.getMessage());
        }
    }


    private void validate(Coupons coupons, SellerModifyCouponRequestDTO dto) {
        if (coupons.getDiscountType() == DiscountType.PERCENT) {
            if (dto.discountValue() < 0 || dto.discountValue() > 100) {
                throw new IllegalArgumentException("Discount value must be between 0 and 100");
            }
        }

        if (coupons.getDiscountType() == DiscountType.AMOUNT) {
            if (dto.discountValue() <= 0) {
                throw new IllegalArgumentException("Discount value must be greater than 0");
            }
        }
    }
}
