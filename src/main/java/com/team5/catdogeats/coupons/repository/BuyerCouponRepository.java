package com.team5.catdogeats.coupons.repository;

import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.mapping.BuyerCoupons;
import com.team5.catdogeats.orders.dto.GroupSellerAndCouponsDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.ZonedDateTime;
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
    FROM   BuyerCoupons bc
    JOIN   bc.coupons   c
    LEFT JOIN SellerCoupons sc
           ON sc.coupons = c
    WHERE  bc.buyers          = :buyer
      AND  c.id               IN :couponIds
      AND  bc.isUsed          = false
      AND  c.startDate       <= :today
      AND  c.endDate         >= :today
""")
    List<GroupSellerAndCouponsDTO> findValidCoupons(@Param("buyer")     Buyers      buyer,
                                                    @Param("couponIds") List<String> couponIds,
                                                    @Param("today") LocalDate today);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE BuyerCoupons bc
        SET    bc.isUsed = true,
               bc.usedAt = :useedAt
        WHERE  bc.buyers = :buyer
          AND  bc.coupons.id IN :couponIds
          AND  bc.isUsed = false
    """)
    int markBuyerCouponUsed(@Param("buyer") Buyers buyer,
                 @Param("couponIds") List<String> couponIds,
                 @Param("useedAt") ZonedDateTime useedAt);
}
