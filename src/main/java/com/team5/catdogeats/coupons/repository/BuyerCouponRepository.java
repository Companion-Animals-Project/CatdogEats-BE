package com.team5.catdogeats.coupons.repository;

import com.team5.catdogeats.coupons.domain.mapping.BuyerCoupons;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuyerCouponRepository extends JpaRepository<BuyerCoupons, String> {
}
