package com.team5.catdogeats.coupons.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.dto.SellerCreateCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.enums.DiscountType;
import com.team5.catdogeats.coupons.domain.mapping.SellerCoupons;
import com.team5.catdogeats.coupons.repository.CouponRepository;
import com.team5.catdogeats.coupons.repository.SellerCouponRepository;
import com.team5.catdogeats.coupons.service.SellerCouponService;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerCouponServiceImpl implements SellerCouponService {
    private final SellerCouponRepository sellerCouponRepository;
    private final CouponRepository couponRepository;
    private final SellersRepository sellersRepository;

    @Override
    public void createCoupon(UserPrincipal userPrincipal, SellerCreateCouponRequestDTO dto) {
        try {
            validate(dto);

            SellerDTO sellerDTO = sellersRepository.findSellerDtoByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                    .orElseThrow(() -> new NoSuchElementException("Seller not found"));
            Sellers seller = SellerDTO.toEntity(sellerDTO);
            Coupons coupon = couponRepository.save(SellerCreateCouponRequestDTO.buildDTO(dto));
            SellerCoupons sellerCoupon = SellerCoupons.builder()
                    .sellers(seller)
                    .coupons(coupon)
                    .build();
            sellerCouponRepository.save(sellerCoupon);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Coupon already exists" + dto.code());
        } catch (Exception e) {
            log.error("Error creating coupon: {}", e.getMessage());
            throw new RuntimeException("Error creating coupon: " + e.getMessage());
        }
    }


    private void validate(SellerCreateCouponRequestDTO dto) {
        if (dto.discountType() == DiscountType.PERCENT) {
            if (dto.discountValue() < 0 || dto.discountValue() > 100) {
                throw new IllegalArgumentException("Discount value must be between 0 and 100");
            }
        }

        if (dto.discountType() == DiscountType.AMOUNT) {
            if (dto.discountValue() <= 0) {
                throw new IllegalArgumentException("Discount value must be greater than 0");
            }
        }
    }
}
