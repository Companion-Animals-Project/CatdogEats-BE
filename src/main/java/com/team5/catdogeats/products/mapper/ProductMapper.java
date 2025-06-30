package com.team5.catdogeats.products.mapper;

import com.team5.catdogeats.products.domain.dto.MyProductResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("""
    <script>
    SELECT
        p.id as productId,
        p.title as productName,
        COALESCE(COUNT(r.id), 0) as reviewCount,
        COALESCE(AVG(r.star), 0.0) as averageStar,
        img.image_id as imageId,
        img.image_url as imageUrl
    FROM products p
    LEFT JOIN reviews r ON r.product_id = p.id
    LEFT JOIN (
        SELECT pi.product_id, pi.product_image_id as image_id, i.image_url
        FROM products_images pi
        LEFT JOIN images i ON pi.product_image_id = i.id
        WHERE (pi.product_id, pi.created_at) IN (
            SELECT pi2.product_id, MIN(pi2.created_at)
            FROM products_images pi2
            GROUP BY pi2.product_id
        )
    ) img ON img.product_id = p.id
    WHERE p.seller_id = #{sellerId}
    GROUP BY p.id, p.title, img.image_id, img.image_url
    <if test="orderBy != null and orderBy != ''">
        ORDER BY ${orderBy}
    </if>
    LIMIT #{limit} OFFSET #{offset}
    </script>
    """)
    List<MyProductResponseDto> findProductSummariesBySellerId(
            @Param("sellerId") String sellerId,
            @Param("orderBy") String orderBy,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    @Select("""
    <script>
    SELECT COUNT(*)
    FROM products p
    WHERE p.seller_id = #{sellerId}
    </script>
    """)
    long countProductsBySellerId(@Param("sellerId") String sellerId);
}
