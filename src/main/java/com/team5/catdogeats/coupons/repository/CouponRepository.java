package com.team5.catdogeats.coupons.repository;

import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.dto.BuyerCouponDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupons, String> {
    @Modifying
    @Query("DELETE FROM Coupons c WHERE c = :coupon")
    void deleteCoupon(@Param("coupon") Coupons coupon);

    @Query("""
    SELECT new com.team5.catdogeats.coupons.domain.dto.BuyerCouponDTO(c, b)
    FROM Coupons c, Buyers b
    WHERE c.code = :code
      AND EXISTS (
        SELECT 1
        FROM Users u
        WHERE u = b.user
          AND u.provider = :provider
          AND u.providerId = :providerId
      )
""")
    Optional<BuyerCouponDTO> findCodeByProviderAndProviderId(
            @Param("code") String code,
            @Param("provider") String provider,
            @Param("providerId") String providerId
    );

}
