package com.team5.catdogeats.reviews.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewMapper {

    // 1. 페이징용 리뷰 id 조회
    @Select("""
    <script>
    SELECT r.id
    FROM reviews r
    JOIN products p ON r.product_id = p.id
    WHERE p.product_number = #{productNumber}
    ORDER BY r.updated_at DESC
    LIMIT #{size} OFFSET #{offset}
    </script>
    """)
    List<String> findReviewIdsByProductNumber(
            @Param("productNumber") Long productNumber,
            @Param("offset") int offset,
            @Param("size") int size
    );

    // 2. 리뷰 id 리스트로 전체 row flat join 조회
    @Select("""
    <script>
    SELECT
        r.id as reviewId,
        u.name as writerName,
        b.name_masking_status as nameMaskingStatus,
        r.star,
        r.contents,
        r.updated_at as updatedAt,
        pet.breed as petBreed,
        pet.age as petAge,
        pet.gender as petGender,
        ri.id as imageId,
        i.image_url as imageUrl
    FROM reviews r
        JOIN buyers b ON r.buyer_id = b.user_id
        JOIN users u ON b.user_id = u.id
        JOIN products p ON r.product_id = p.id
        LEFT JOIN pets pet ON pet.buyer_id = b.user_id
        LEFT JOIN reviews_images ri ON ri.review_id = r.id
        LEFT JOIN images i ON ri.review_image_id = i.id
    WHERE r.id IN
    <foreach collection="reviewIds" item="reviewId" open="(" separator="," close=")">
        #{reviewId}
    </foreach>
    ORDER BY r.updated_at DESC
    </script>
    """)
    List<FlatReviewRow> findReviewFlatRowsByIds(
            @Param("reviewIds") List<String> reviewIds
    );

    @Select("""
    SELECT COUNT(*)
    FROM reviews r
    JOIN products p ON r.product_id = p.id
    WHERE p.product_number = #{productNumber}
    """)
    long countReviewsByProductNumber(@Param("productNumber") Long productNumber);

    // Flat row 결과용 임시 DTO
    class FlatReviewRow {
        public String reviewId;
        public String writerName;
        public Boolean nameMaskingStatus;
        public Double star;
        public String contents;
        public String updatedAt;
        public String petBreed;
        public Short petAge;
        public String petGender;
        public String imageId;
        public String imageUrl;
    }
}
