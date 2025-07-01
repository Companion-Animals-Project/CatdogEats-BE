package com.team5.catdogeats.coupons.mapper;

import com.team5.catdogeats.coupons.domain.Coupons;
import com.team5.catdogeats.coupons.domain.dto.BuyerCouponDTO;
import com.team5.catdogeats.coupons.domain.dto.SellerCouponListResponseDTO;
import com.team5.catdogeats.coupons.domain.enums.DiscountType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Mapper
public interface CouponMapper {
    @Select("""
    SELECT c.*
    FROM coupons c
    INNER JOIN seller_coupons sc ON sc.coupon_id = c.id
    INNER JOIN sellers s ON s.user_id = sc.seller_id
    INNER JOIN users u ON u.id = s.user_id
    WHERE c.id = #{id}
      AND u.provider = #{provider}
      AND u.provider_id = #{providerId}
""")
    Optional<Coupons> findCouponByIdAndProviderAndProviderId(@Param("id") String id,
                                                            @Param("provider") String provider,
                                                            @Param("providerId") String providerId);

    @Update("""
            <script>
            UPDATE coupons
            <set>
            <if test='couponName != null and couponName.trim() != ""'> coupon_name = #{couponName}, </if>
            <if test='code != null and code.trim() != ""'> code = #{code}, </if>
            <if test='discountType != null'> discount_type = #{discountType}, </if>
            <if test='discountValue != null'> discount_value = #{discountValue}, </if>
            <if test='startDate != null'> start_date = #{startDate}, </if>
            <if test='endDate != null'> end_date = #{endDate}, </if>
            <if test='usageLimit != null'> usage_limit = #{usageLimit}, </if>
            updated_at = now()
            </set>
            WHERE id = #{id}
            </script>
    """)
    int updateCoupons(
            @Param("id") String id,
            @Param("couponName") String couponName,
            @Param("code") String code,
            @Param("discountType") DiscountType discountType,
            @Param("discountValue") Integer discountValue,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("usageLimit") Integer usageLimit
    );


    @Select("""
        SELECT
                c.id,
                c.code,
                c.coupon_name AS couponName,
                c.discount_type AS discountType,
                c.discount_value AS discountValue,
                c.start_date AS startDate,
                c.end_date AS endDate,
                c.usage_limit AS usageLimit
            FROM coupons c
            INNER JOIN seller_coupons sc ON sc.coupon_id = c.id
            INNER JOIN sellers s ON s.user_id = sc.seller_id
            INNER JOIN users u ON u.id = s.user_id
            WHERE u.provider = #{provider}
              AND u.provider_id = #{providerId}
            ORDER BY c.created_at DESC
            LIMIT #{limit}
            OFFSET #{offset}
    """)
    List<SellerCouponListResponseDTO> findCouponsByProviderAndProviderId(@Param("provider") String provider,
                                                                         @Param("providerId") String providerId,
                                                                         @Param("limit") int limit,
                                                                         @Param("offset") int offset);

    @Select("""
        SELECT
                c.id,
                c.code,
                c.coupon_name AS couponName,
                c.discount_type AS discountType,
                c.discount_value AS discountValue,
                c.start_date AS startDate,
                c.end_date AS endDate,
                c.usage_limit AS usageLimit
            FROM coupons c
            INNER JOIN seller_coupons sc ON sc.coupon_id = c.id
            INNER JOIN sellers s ON s.vendor_name = #{vendorName}
            ORDER BY c.created_at DESC
            LIMIT #{limit}
            OFFSET #{offset}
    """)
    List<SellerCouponListResponseDTO> findCouponsByVendorName(@Param("vendorName") String vendorName,
                                                             @Param("limit") int limit,
                                                             @Param("offset") int offset);

}
