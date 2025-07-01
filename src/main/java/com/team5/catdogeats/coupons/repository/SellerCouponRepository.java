package com.team5.catdogeats.coupons.repository;

import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.dto.SellerCouponDTO;
import com.team5.catdogeats.coupons.domain.mapping.SellerCoupons;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;


public interface SellerCouponRepository extends JpaRepository<SellerCoupons, String> {

    @Modifying
    @Query("DELETE FROM SellerCoupons sc WHERE sc.coupons = :coupon")
    void deleteByCoupons(@Param("coupon") Coupons coupon);


    @Query("""
        SELECT 1
        FROM SellerCoupons sc
        WHERE sc.sellers = :sellers AND sc.coupons = :coupons
    """)
    Integer existsRaw(@Param("sellers") Sellers sellers,
                      @Param("coupons") Coupons coupons);


    @Query("""
    SELECT new com.team5.catdogeats.coupons.domain.dto.SellerCouponDTO(c, s)
    FROM SellerCoupons sc
    JOIN sc.coupons c
    JOIN sc.sellers s
    WHERE c.code = :code
      AND s = :sellers
""")
    Optional<SellerCouponDTO> findByCodeAndSellers(
            @Param("code") String code,
            @Param("sellers") Sellers sellers
    );
}
