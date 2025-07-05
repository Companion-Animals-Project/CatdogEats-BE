package com.team5.catdogeats.coupons.mapper;

import com.team5.catdogeats.coupons.domain.dto.BuyerCouponCountDTO;
import com.team5.catdogeats.coupons.domain.dto.BuyerCouponSelectedDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface BuyerCouponMapper {

    @Select("""
    <script>
    SELECT
        c.id,
        c.code,
        c.coupon_name AS couponName,
        c.discount_type AS discountType,
        c.discount_value AS discountValue,
        c.start_date AS startDate,
        c.end_date AS endDate,
        bc.is_used AS isUsed
    FROM buyer_coupons bc
    INNER JOIN coupons c ON bc.coupon_id = c.id
    INNER JOIN buyers b ON bc.buyer_id = b.user_id
    INNER JOIN users u ON b.user_id = u.id
    WHERE u.provider = #{provider}
      AND u.provider_id = #{providerId}
    
        <if test='filter == "AVAILABLE"'>
            AND bc.is_used = false  
            AND CURRENT_DATE BETWEEN c.start_date AND c.end_date
        </if>
        <if test='filter == "EXPIRING"'>
            AND bc.is_used = false
            AND CURRENT_DATE BETWEEN c.start_date AND c.end_date
            AND c.end_date &lt;= CURRENT_DATE + INTERVAL '3 days'
        </if>
        <if test='filter == "USED_OR_EXPIRED"'>
            AND (bc.is_used = true OR c.end_date &lt; CURRENT_DATE)
        </if>

    ORDER BY c.created_at DESC
    LIMIT #{limit}
    OFFSET #{offset}
    </script>
""")
    List<BuyerCouponSelectedDTO> findBuyerCoupons(
            @Param("provider") String provider,
            @Param("providerId") String providerId,
            @Param("filter") String filter,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Select("""
SELECT
    COALESCE(COUNT(CASE 
        WHEN bc.is_used = false
             AND CURRENT_DATE BETWEEN c.start_date AND c.end_date
             AND c.end_date > CURRENT_DATE + INTERVAL '3 days' 
 
        THEN 1
        ELSE null 
    END), 0) AS availableCount,

    COALESCE(COUNT(CASE 
        WHEN bc.is_used = false
             AND CURRENT_DATE BETWEEN c.start_date AND c.end_date
             AND c.end_date <= CURRENT_DATE + INTERVAL '3 days' 
        THEN 1 ELSE null 
    END), 0) AS expiringSoonCount
FROM buyer_coupons bc
JOIN coupons c ON bc.coupon_id = c.id
JOIN buyers b ON bc.buyer_id = b.user_id
JOIN users u ON b.user_id = u.id
WHERE u.provider = #{provider}
  AND u.provider_id = #{providerId}
""")
    BuyerCouponCountDTO countUsedFalseAndExpiringCoupons(
            @Param("provider") String provider,
            @Param("providerId") String providerId
    );

}
