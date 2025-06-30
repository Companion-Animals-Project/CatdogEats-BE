package com.team5.catdogeats.coupons.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.dto.SellerCouponListResponseDTO;
import com.team5.catdogeats.coupons.mapper.CouponMapper;
import com.team5.catdogeats.coupons.service.SellerCouponListService;
import com.team5.catdogeats.global.config.MybatisTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerCouponListServiceImpl implements SellerCouponListService {
    private final CouponMapper couponMapper;

    @Override
    @MybatisTransactional(readOnly = true)
    public List<SellerCouponListResponseDTO> getSellerCoupons(UserPrincipal userPrincipal, int page, int size) {
        int offset = page * size;
        return couponMapper.findCouponsByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId(),
                size,
                offset
        );
    }
}
