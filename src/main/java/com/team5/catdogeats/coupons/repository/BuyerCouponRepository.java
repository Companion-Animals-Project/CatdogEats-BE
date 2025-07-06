package com.team5.catdogeats.coupons.repository;

import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.mapping.BuyerCoupons;
import com.team5.catdogeats.orders.dto.GroupSellerAndCouponsDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BuyerCouponRepository extends JpaRepository<BuyerCoupons, String> {

    @Query("""
        SELECT 1
        FROM BuyerCoupons bc
        WHERE bc.buyers = :buyers AND bc.coupons = :coupons
    """)
    Integer existsRaw(@Param("buyers") Buyers buyers,
                     @Param("coupons") Coupons coupons);

    @Query("""
    SELECT new com.team5.catdogeats.orders.dto.GroupSellerAndCouponsDTO(c, sc.sellers)
    FROM BuyerCoupons bc
    JOIN bc.coupons c
    LEFT JOIN SellerCoupons sc ON sc.coupons = c
    WHERE bc.buyers = :buyers
      AND c.id IN :couponIds
""")
    List<GroupSellerAndCouponsDTO> findAllByBuyerAndCouponIds(@Param("buyers") Buyers buyers, @Param("couponIds") List<String> couponIds);


}
