package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.dto.ProductDetailProjection;
import com.team5.catdogeats.products.domain.dto.ProductListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Products, String> {
    Optional<Products> findById(String productId);

    Optional<Products> findByProductNumber(Long productNumber);

    Boolean existsByProductNumber(Long productNumber);

    void deleteById(String productId);

    // 단순 스토어 상품 개수 조회
    @Query("SELECT COUNT(p) FROM Products p WHERE p.seller.userId = :sellerId")
    Long countSellerActiveProducts(@Param("sellerId") String sellerId);

    // 최신순 정렬
    @Query(value = """
        SELECT 
            p.id AS productId,
            (
                SELECT i.image_url
                FROM products_images pi
                JOIN images i ON i.id = pi.product_image_id
                WHERE pi.product_id = p.id
                ORDER BY pi.created_at
                LIMIT 1
            ) AS imageUrl,
            p.title AS productName,
            s.vendor_name AS vendorName,
            (
                SELECT AVG(r.star)
                FROM reviews r
                WHERE r.product_id = p.id
            ) AS averageStar,
            p.price,
            p.discount_rate AS discountRate,
            p.is_discounted AS isDiscounted,
            p.petCategory,
            p.productCategory,
            p.stock_status AS stockStatus,
            p.created_at AS createdAt
        FROM products p
        JOIN sellers s ON s.user_id = p.seller_id
        WHERE (:petCategory IS NULL OR p.petCategory = :petCategory)
          AND (:productCategory IS NULL OR p.productCategory = :productCategory)
        ORDER BY p.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM products p
        WHERE (:petCategory IS NULL OR p.petCategory = :petCategory)
          AND (:productCategory IS NULL OR p.productCategory = :productCategory)
        """,
            nativeQuery = true
    )
    Page<ProductListProjection> findAllByOrderByCreatedAtDesc(
            @Param("petCategory") String petCategory,
            @Param("productCategory") String productCategory,
            Pageable pageable
    );

    // 가격순 정렬
    @Query(value = """
        SELECT 
            p.id AS productId,
            (
                SELECT i.image_url
                FROM products_images pi
                JOIN images i ON i.id = pi.product_image_id
                WHERE pi.product_id = p.id
                ORDER BY pi.created_at
                LIMIT 1
            ) AS imageUrl,
            p.title AS productName,
            s.vendor_name AS vendorName,
            (
                SELECT AVG(r.star)
                FROM reviews r
                WHERE r.product_id = p.id
            ) AS averageStar,
            p.price,
            p.discount_rate AS discountRate,
            p.is_discounted AS isDiscounted,
            p.petCategory,
            p.productCategory,
            p.stock_status AS stockStatus,
            p.created_at AS createdAt
        FROM products p
        JOIN sellers s ON s.user_id = p.seller_id
        WHERE (:petCategory IS NULL OR p.petCategory = :petCategory)
          AND (:productCategory IS NULL OR p.productCategory = :productCategory)
        ORDER BY p.price DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM products p
        WHERE (:petCategory IS NULL OR p.petCategory = :petCategory)
          AND (:productCategory IS NULL OR p.productCategory = :productCategory)
        """,
            nativeQuery = true
    )
    Page<ProductListProjection> findAllByOrderByPriceDesc(
            @Param("petCategory") String petCategory,
            @Param("productCategory") String productCategory,
            Pageable pageable
    );

    // 별점순 정렬
    @Query(value = """
        SELECT 
            p.id AS productId,
            (
                SELECT i.image_url
                FROM products_images pi
                JOIN images i ON i.id = pi.product_image_id
                WHERE pi.product_id = p.id
                ORDER BY pi.created_at
                LIMIT 1
            ) AS imageUrl,
            p.title AS productName,
            s.vendor_name AS vendorName,
            (
                SELECT AVG(r.star)
                FROM reviews r
                WHERE r.product_id = p.id
            ) AS averageStar,
            p.price,
            p.discount_rate AS discountRate,
            p.is_discounted AS isDiscounted,
            p.petCategory,
            p.productCategory,
            p.stock_status AS stockStatus,
            p.created_at AS createdAt
        FROM products p
        JOIN sellers s ON s.user_id = p.seller_id
        WHERE (:petCategory IS NULL OR p.petCategory = :petCategory)
          AND (:productCategory IS NULL OR p.productCategory = :productCategory)
        ORDER BY (
        SELECT AVG(r.star)
        FROM reviews r
        WHERE r.product_id = p.id
        ) DESC NULLS LAST
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM products p
        WHERE (:petCategory IS NULL OR p.petCategory = :petCategory)
          AND (:productCategory IS NULL OR p.productCategory = :productCategory)
        """,
            nativeQuery = true
    )
    Page<ProductListProjection> findAllByOrderByAverageStarDesc(
            @Param("petCategory") String petCategory,
            @Param("productCategory") String productCategory,
            Pageable pageable
    );

    @Query(
            value = """
            SELECT 
                p.title AS title,
                p.subtitle AS subTitle,
                p.productinfo AS productInfo,
                p.contents AS contents,
                p.is_discounted AS isDiscounted,
                p.discount_rate AS discountRate,
                p.price AS price,
                -- 이미지 여러 장 json 배열로
                (
                  SELECT COALESCE(json_agg(i.image_url), '[]')
                  FROM products_images pi
                  JOIN images i ON i.id = pi.product_image_id
                  WHERE pi.product_id = p.id
                ) AS images,
                s.vendor_name AS vendorName,
                (
                  SELECT AVG(r.star)::float
                  FROM reviews r
                  WHERE r.product_id = p.id
                ) AS averageStar,
                (
                  SELECT COUNT(*)
                  FROM reviews r
                  WHERE r.product_id = p.id
                ) AS reviewCount
            FROM products p
            JOIN sellers s ON s.user_id = p.seller_id
            WHERE p.product_number = :productNumber
            LIMIT 1
        """,
            nativeQuery = true
    )
    ProductDetailProjection findProductDetailByProductNumber(@Param("productNumber") Long productNumber);

}
