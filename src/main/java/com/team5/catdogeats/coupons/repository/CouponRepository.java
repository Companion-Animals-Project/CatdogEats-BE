package com.team5.catdogeats.coupons.repository;

import com.team5.catdogeats.coupons.domain.Coupons;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupons, String> {
    @Modifying
    @Query("DELETE FROM Coupons c WHERE c = :coupon")
    void deleteCoupon(@Param("coupon") Coupons coupon);

}
