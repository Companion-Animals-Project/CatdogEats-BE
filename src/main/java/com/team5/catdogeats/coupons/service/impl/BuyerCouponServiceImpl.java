package com.team5.catdogeats.coupons.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.dto.*;
import com.team5.catdogeats.coupons.domain.enums.CouponFilterType;
import com.team5.catdogeats.coupons.exception.DuplicateCouponException;
import com.team5.catdogeats.coupons.mapper.BuyerCouponMapper;
import com.team5.catdogeats.coupons.repository.BuyerCouponRepository;
import com.team5.catdogeats.coupons.repository.CouponRepository;
import com.team5.catdogeats.coupons.service.BuyerCouponService;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                Integer result = buyerCouponRepository.existsRaw(buyerDTO.buyers(), buyerDTO.coupons());
                if (result != null) {
                    log.warn("중복 쿠폰 발행 시도");
                    throw new DuplicateCouponException("이미 보유한 쿠폰입니다.");
                }
                buyerCouponRepository.save(BuyerCouponDTO.toEntity(buyerDTO));
            } catch (NoSuchElementException e) {
                log.warn("쿠폰 정보 없음: {}", dto.code());
                throw e;
            }

    }

    @Override
    @MybatisTransactional(readOnly = true)
    public BuyerCouponListResponseDTO getBuyerCoupons(UserPrincipal userPrincipal, CouponFilterType filter, int page, int size) {
        try {
            int offset = page * size;
            List<BuyerCouponSelectedDTO> selectedDTOList = buyerCouponMapper.findBuyerCoupons(
                    userPrincipal.provider(),
                    userPrincipal.providerId(),
                    filter.toString(),
                    size,
                    offset
            );

            BuyerCouponCountDTO couponCountDTO = buyerCouponMapper.countUsedFalseAndExpiringCoupons(userPrincipal.provider(),
                                                                                                    userPrincipal.providerId());
            return new BuyerCouponListResponseDTO(couponCountDTO, selectedDTOList);
        } catch (Exception e) {
            log.error("Error while getting buyer coupons {}", e.getMessage());
            throw new RuntimeException("Error getting buyer coupons: " + e.getMessage());
        }

    }

}
