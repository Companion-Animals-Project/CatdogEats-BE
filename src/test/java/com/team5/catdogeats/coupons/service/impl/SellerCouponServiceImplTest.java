package com.team5.catdogeats.coupons.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.dto.SellerCreateCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.enums.DiscountType;
import com.team5.catdogeats.coupons.exception.DuplicateCouponException;
import com.team5.catdogeats.coupons.repository.CouponRepository;
import com.team5.catdogeats.coupons.repository.SellerCouponRepository;
import com.team5.catdogeats.coupons.service.SellerCouponService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("판매자 쿠폰 서비스 통합 테스트")
class SellerCouponServiceImplTest {

    @Autowired
    private SellerCouponService sellerCouponService;

    @Autowired
    private SellersRepository sellersRepository;

    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SellerCouponRepository sellerCouponRepository;

    private UserPrincipal userPrincipal;
    private SellerCreateCouponRequestDTO createCouponRequest;
    private Sellers seller;
    private Users users;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal("google", "12345");

        users = Users.builder()
                .provider("google")
                .providerId("12345")
                .name("test")
                .userNameAttribute("sub")
                .role(Role.ROLE_SELLER)
                .accountDisable(false)
                .deletedAt(null)
                .build();
        users = userRepository.save(users);

        seller = Sellers.builder()
                .user(users)
                .vendorName("Test Vendor")
                .build();

        sellersRepository.save(seller);

        createCouponRequest = new SellerCreateCouponRequestDTO(
                "Test Coupon",
                "COUPON123",
                DiscountType.PERCENT,
                20,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                100
        );
    }

    @Test
    @DisplayName("쿠폰 생성 성공 - 퍼센트 할인")
    void createCoupon_Success_PercentDiscount() {
        assertThatNoException().isThrownBy(() ->
                sellerCouponService.createCoupon(userPrincipal, createCouponRequest));

        assertThat(couponRepository.findAll()).hasSize(1);
        assertThat(sellerCouponRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("쿠폰 생성 실패 - 중복 쿠폰")
    void createCoupon_DuplicateCoupon() {
        sellerCouponService.createCoupon(userPrincipal, createCouponRequest);

        assertThatThrownBy(() ->
                sellerCouponService.createCoupon(userPrincipal, createCouponRequest))
                .isInstanceOf(DuplicateCouponException.class);
    }

    @Test
    @DisplayName("쿠폰 생성 실패 - 판매자 없음")
    void createCoupon_SellerNotFound() {
        // 판매자 삭제
        sellersRepository.deleteAll();

        assertThatThrownBy(() ->
                sellerCouponService.createCoupon(userPrincipal, createCouponRequest))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("쿠폰 생성 실패 - 할인값 음수")
    void createCoupon_InvalidDiscount() {
        SellerCreateCouponRequestDTO invalid = new SellerCreateCouponRequestDTO(
                "Invalid", "INVALID", DiscountType.PERCENT, -10,
                LocalDate.now(), LocalDate.now().plusDays(10), 50
        );

        assertThatThrownBy(() ->
                sellerCouponService.createCoupon(userPrincipal, invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Discount value");
    }
}