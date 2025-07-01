package com.team5.catdogeats.coupons.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.dto.BuyerCouponListResponseDTO;
import com.team5.catdogeats.coupons.domain.dto.BuyerCreateCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.enums.CouponFilterType;
import com.team5.catdogeats.coupons.domain.enums.DiscountType;
import com.team5.catdogeats.coupons.exception.DuplicateCouponException;
import com.team5.catdogeats.coupons.repository.BuyerCouponRepository;
import com.team5.catdogeats.coupons.repository.CouponRepository;
import com.team5.catdogeats.coupons.service.BuyerCouponService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BuyerCouponServiceImplTest {

    @Autowired
    private BuyerCouponService buyerCouponService;
    @Autowired
    private BuyerCouponRepository buyerCouponRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private BuyerRepository buyerRepository;
    @Autowired
    private UserRepository userRepository;


    private UserPrincipal userPrincipal;
    private BuyerCreateCouponRequestDTO createCouponRequest;
    private Coupons coupon;
    private Buyers buyer;
    private Users users;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal("google", "12345");
        createCouponRequest = new BuyerCreateCouponRequestDTO("COUPON123");



        users = Users.builder()
                .provider("google")
                .providerId("12345")
                .name("test")
                .userNameAttribute("sub")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build();
        users = userRepository.save(users);


        buyer = Buyers.builder()
                .user(users)
                .nameMaskingStatus(true)
                .build();

        buyer = buyerRepository.save(buyer);
        coupon = Coupons.builder()
                .couponName("Test Coupon")
                .code("COUPON123")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .discountType(DiscountType.PERCENT)
                .discountValue(14)
                .build();
        couponRepository.save(coupon);
    }

    @Test
    @DisplayName("쿠폰 생성 성공")
    void createCoupon_Success() {

        // when
        assertThatNoException().isThrownBy(() ->
                buyerCouponService.createCoupon(userPrincipal, createCouponRequest));

        // then
        assertThat(buyerCouponRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("쿠폰 생성 실패 - 쿠폰 정보 없음")
    void createCoupon_CouponNotFound() {
        BuyerCreateCouponRequestDTO notExistRequest = new BuyerCreateCouponRequestDTO("NOT_EXIST");

        // when & then
        assertThatThrownBy(() ->
                buyerCouponService.createCoupon(userPrincipal, notExistRequest))
                .isInstanceOf(NoSuchElementException.class);

        assertThat(buyerCouponRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("쿠폰 생성 실패 - 중복 쿠폰")
    void createCoupon_DuplicateCoupon() {
        // given
        // 1. 첫 번째 쿠폰을 생성하여 DB에 저장합니다.
        buyerCouponService.createCoupon(userPrincipal, createCouponRequest);


        assertThatThrownBy(() ->
                buyerCouponService.createCoupon(userPrincipal, createCouponRequest))
                .isInstanceOf(DuplicateCouponException.class);


    }

    @Test
    @DisplayName("구매자 쿠폰 목록 조회 성공")
    void getBuyerCoupons_Success() {
        buyerCouponService.createCoupon(userPrincipal, createCouponRequest);

        List<BuyerCouponListResponseDTO> result = buyerCouponService.getBuyerCoupons(
                userPrincipal, CouponFilterType.AVAILABLE, 0, 10);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("구매자 쿠폰 목록 조회 실패")
    void getBuyerCoupons_Exception() {
        assertThatThrownBy(() ->
                buyerCouponService.getBuyerCoupons(null, CouponFilterType.ALL, 0, 10))
                .isInstanceOf(RuntimeException.class);
    }
}