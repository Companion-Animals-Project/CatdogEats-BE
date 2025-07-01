package com.team5.catdogeats.coupons.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.dto.SellerDeleteCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.dto.SellerModifyCouponRequestDTO;
import com.team5.catdogeats.coupons.domain.enums.DiscountType;
import com.team5.catdogeats.coupons.domain.mapping.SellerCoupons;
import com.team5.catdogeats.coupons.mapper.CouponMapper;
import com.team5.catdogeats.coupons.repository.CouponRepository;
import com.team5.catdogeats.coupons.repository.SellerCouponRepository;
import com.team5.catdogeats.coupons.service.SellerUpdateCouponService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class SellerUpdateCouponServiceImplTest {
    @Autowired
    private SellerUpdateCouponService sellerUpdateCouponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private SellerCouponRepository sellerCouponRepository;

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private SellersRepository sellersRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;
    private Sellers seller;
    private Coupons coupon;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        Users user = Users.builder()
                .provider("google")
                .providerId("12345")
                .name("판매자")
                .userNameAttribute("sub")
                .role(Role.ROLE_SELLER)
                .accountDisable(false)
                .build();
        userRepository.save(user);

        seller = Sellers.builder()
                .user(user)
                .vendorName("테스트벤더")
                .build();
        sellersRepository.save(seller);

        coupon = Coupons.builder()
                .code("COUPON123")
                .couponName("기존쿠폰")
                .discountType(DiscountType.PERCENT)
                .discountValue(10)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(7))
                .build();
        coupon = couponRepository.save(coupon);

        sellerCouponRepository.save(
                SellerCoupons.builder()
                        .coupons(coupon)
                        .sellers(seller)
                        .build()
        );

        userPrincipal = new UserPrincipal("google", "12345");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("쿠폰 수정 성공")
    void modifyCoupon_Success() {
        SellerModifyCouponRequestDTO dto = new SellerModifyCouponRequestDTO(
                coupon.getId(),
                "수정된 쿠폰명",
                "NEWCODE123",
                DiscountType.PERCENT,
                15,
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                100
        );

        assertThatNoException().isThrownBy(() ->
                sellerUpdateCouponService.modifierCoupon(userPrincipal, dto));
    }

    @Test
    @DisplayName("쿠폰 삭제 성공")
    void deleteCoupon_Success() {
        SellerDeleteCouponRequestDTO dto = new SellerDeleteCouponRequestDTO(coupon.getId());

        assertThatNoException().isThrownBy(() ->
                sellerUpdateCouponService.deleteCoupon(userPrincipal, dto));


        assertThat(couponRepository.findById(coupon.getId())).isEmpty();
    }

    @Test
    @DisplayName("쿠폰 수정 실패 - 잘못된 할인 값")
    void modifyCoupon_Fail_InvalidDiscount() {
        SellerModifyCouponRequestDTO dto = new SellerModifyCouponRequestDTO(
                coupon.getId(),
                "수정된 쿠폰명",
                "NEWCODE123",
                DiscountType.PERCENT,
                999,  // 잘못된 할인율
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                100
        );

        assertThatThrownBy(() ->
                sellerUpdateCouponService.modifierCoupon(userPrincipal, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("쿠폰 삭제 실패 - 존재하지 않는 쿠폰")
    void deleteCoupon_Fail_NotFound() {
        SellerDeleteCouponRequestDTO dto = new SellerDeleteCouponRequestDTO("non-existent-id");

        assertThatThrownBy(() ->
                sellerUpdateCouponService.deleteCoupon(userPrincipal, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("쿠폰 정보를 찾을 수 없습니다.");
    }
}