package com.team5.catdogeats.coupons.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.dto.BuyerCouponDTO;
import com.team5.catdogeats.coupons.domain.dto.BuyerCouponListResponseDTO;
import com.team5.catdogeats.coupons.domain.dto.BuyerCreateCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.enums.CouponFilterType;
import com.team5.catdogeats.coupons.mapper.BuyerCouponMapper;
import com.team5.catdogeats.coupons.repository.BuyerCouponRepository;
import com.team5.catdogeats.coupons.repository.CouponRepository;
import com.team5.catdogeats.coupons.service.BuyerCouponService;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuyerCouponServiceImpl implements BuyerCouponService {
    private final BuyerCouponRepository buyerCouponRepository;
    private final CouponRepository couponRepository;
    private final BuyerCouponMapper buyerCouponMapper;

    @Override
    public void createCoupon(UserPrincipal userPrincipal, BuyerCreateCouponRequestDTO dto) {
        try {
            BuyerCouponDTO buyerDTO = couponRepository.findCodeByProviderAndProviderId(dto.code(), userPrincipal.provider(), userPrincipal.providerId())
                    .orElseThrow(() -> new NoSuchElementException("쿠폰 정보를 찾을 수 없습니다."));
            log.debug("dto: {}", dto);
            log.debug("Found coupon: {}", buyerDTO);
            buyerCouponRepository.save(BuyerCouponDTO.toEntity(buyerDTO));
        } catch (DataIntegrityViolationException e) {
            log.warn("이미 존재하는 쿠폰입니다. {}", dto.code());
            throw new IllegalStateException("이미 존재하는 쿠폰입니다.");
        }  catch (Exception e) {
            log.error("Error while creating buyer coupon {}", e.getMessage());
            throw new RuntimeException("Error creating coupon: " + e.getMessage());
        }
    }

    @Override
    @MybatisTransactional(readOnly = true)
    public List<BuyerCouponListResponseDTO> getBuyerCoupons(UserPrincipal userPrincipal, CouponFilterType filter, int page, int size) {
        try {
            int offset = page * size;
            return buyerCouponMapper.findBuyerCoupons(
                    userPrincipal.provider(),
                    userPrincipal.providerId(),
                    filter.toString(),
                    size,
                    offset
            );
        } catch (Exception e) {
            log.error("Error while getting buyer coupons {}", e.getMessage());
            throw new RuntimeException("Error getting buyer coupons: " + e.getMessage());
        }

    }

}
