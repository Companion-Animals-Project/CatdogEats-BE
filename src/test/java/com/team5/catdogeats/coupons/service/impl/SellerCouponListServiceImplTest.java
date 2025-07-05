package com.team5.catdogeats.coupons.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.dto.SellerCouponListResponseDTO;
import com.team5.catdogeats.coupons.domain.enums.DiscountType;
import com.team5.catdogeats.coupons.domain.mapping.SellerCoupons;
import com.team5.catdogeats.coupons.repository.CouponRepository;
import com.team5.catdogeats.coupons.repository.SellerCouponRepository;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SellerCouponListServiceImplTest {

    @Autowired
    private SellerCouponListServiceImpl sellerCouponListService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private SellerCouponRepository sellerCouponRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SellersRepository sellersRepository;
    @Autowired
    private EntityManager entityManager;

    private UserPrincipal userPrincipal;
    private Sellers seller;
    private Users user;

    @BeforeEach
    void setUp() {
        user = Users.builder()
                .provider("google")
                .providerId("12345")
                .name("테스트판매자")
                .userNameAttribute("sub")
                .role(Role.ROLE_SELLER)
                .accountDisable(false)
                .build();
        userRepository.save(user);

        seller = Sellers.builder()
                .user(user)
                .vendorName("테스트상점")
                .build();
        sellersRepository.save(seller);

        Coupons coupon = Coupons.builder()
                .couponName("테스트쿠폰")
                .code("TESTCODE1")
                .discountType(DiscountType.PERCENT)
                .discountValue(10)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .build();
        couponRepository.save(coupon);

        SellerCoupons sellerCoupon = SellerCoupons.builder()
                .sellers(seller)
                .coupons(coupon)
                .build();
        sellerCouponRepository.save(sellerCoupon);

        userPrincipal = new UserPrincipal("google", "12345");

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("판매자 본인 쿠폰 목록 조회 성공")
    void getSellerCoupons_Success() {
        List<SellerCouponListResponseDTO> result = sellerCouponListService.getSellerCoupons(userPrincipal, 0, 10);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).couponName()).isEqualTo("테스트쿠폰");
    }

    @Test
    @DisplayName("상점명으로 쿠폰 목록 조회 성공")
    void getSellerCouponsWithVendorName_Success() {
        List<SellerCouponListResponseDTO> result = sellerCouponListService.getSellerCouponsWithVendorName("테스트상점", 0, 10);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).couponName()).isEqualTo("테스트쿠폰");
    }
}