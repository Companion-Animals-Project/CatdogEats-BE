package com.team5.catdogeats.coupons.repository;

import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.mapping.BuyerCoupons;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BuyerCouponRepository extends JpaRepository<BuyerCoupons, String> {

    @Query("""
        SELECT 1
        FROM BuyerCoupons bc
        WHERE bc.buyers = :buyers AND bc.coupons = :coupons
    """)
    Integer existsRaw(@Param("buyers") Buyers buyers,
                     @Param("coupons") Coupons coupons);
}
