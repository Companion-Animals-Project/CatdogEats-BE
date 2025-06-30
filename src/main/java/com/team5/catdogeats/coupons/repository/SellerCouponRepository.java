package com.team5.catdogeats.coupons.repository;

import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.mapping.SellerCoupons;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface SellerCouponRepository extends JpaRepository<SellerCoupons, String> {

    @Modifying
    @Query("DELETE FROM SellerCoupons sc WHERE sc.coupons = :coupon")
    void deleteByCoupons(@Param("coupon") Coupons coupon);
}
