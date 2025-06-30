package com.team5.catdogeats.reviews.repository;

import com.team5.catdogeats.reviews.domain.Reviews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Reviews, String> {
    void deleteById(String reviewId);

    // 전체 평균/갯수
    @Query("""
        select coalesce(avg(r.star),0.0), count(r)
        from Reviews r
        where r.product.seller.userId = :sellerId
    """)
    List<Object[]> findAvgAndCountBySellerId(@Param("sellerId") String sellerId);

    @Query(value = """
        SELECT
            r.id as review_id,
            p.title as product_name,
            r.star as star,
            r.contents as contents,
            r.updated_at as updated_at,
            i.id as image_id,
            i.image_url as image_url
        FROM reviews r
        JOIN products p ON r.product_id = p.id
        LEFT JOIN reviews_images ri ON ri.review_id = r.id
        LEFT JOIN images i ON ri.review_image_id = i.id
        WHERE r.buyer_id = :buyerId
        ORDER BY r.updated_at DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<Object[]> findReviewsWithImagesAndProductByBuyerNative(
            @Param("buyerId") String buyerId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query("""
        SELECT COUNT(r)
        FROM Reviews r
        WHERE r.buyer.user.id = :buyerId
    """)
    long countByBuyerId(@Param("buyerId") String buyerId);


    // 별점 구간별 개수 (0점대~5점)
    @Query("""
        select
            case 
                when floor(r.star) = 0 then 0
                when floor(r.star) = 1 then 1
                when floor(r.star) = 2 then 2
                when floor(r.star) = 3 then 3
                when floor(r.star) = 4 then 4
                when floor(r.star) >= 5 then 5
            end as groupStar,
            count(r)
        from Reviews r
        where r.product.seller.userId = :sellerId
        group by groupStar
    """)
    List<Object[]> findGroupStarCountBySellerId(@Param("sellerId") String sellerId);
}
