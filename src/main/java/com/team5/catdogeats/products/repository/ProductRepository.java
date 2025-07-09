package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.dto.MainProductProjection;
import com.team5.catdogeats.products.domain.dto.ProductDetailProjection;
import com.team5.catdogeats.products.domain.dto.ProductListProjection;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
                SELECT ROUND(AVG(r.star)::numeric, 1)
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
                SELECT ROUND(AVG(r.star)::numeric, 1)
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
                SELECT ROUND(AVG(r.star)::numeric, 1)
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

    // 상품 상제 정보 조회
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
                    SELECT ROUND(AVG(r.star)::numeric, 1)
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

    // 신상품 - 최신순으로 나열 후 8개 조회
    @Query(
            value = """
        SELECT 
            (
                SELECT i.image_url
                FROM products_images pi
                JOIN images i ON i.id = pi.product_image_id
                WHERE pi.product_id = p.id
                ORDER BY pi.created_at
                LIMIT 1
            ) AS imageUrl,
            s.vendor_name AS vendorName,
            p.title AS title,
            (
                SELECT ROUND(AVG(r.star)::numeric, 1)
                FROM reviews r
                WHERE r.product_id = p.id
            ) AS averageStar,
            (
                SELECT COUNT(*)
                FROM reviews r
                WHERE r.product_id = p.id
            ) AS reviewCount,
            p.price,
            p.is_discounted AS isDiscounted,
            p.discount_rate AS discountRate,
            p.created_at AS createdAt
        FROM products p
        JOIN sellers s ON s.user_id = p.seller_id
        ORDER BY p.created_at DESC
        LIMIT 8
    """,
            nativeQuery = true
    )
    List<MainProductProjection> findTop8ByOrderByCreatedAtDesc();

    // 할인 상품 - 할인율 높은순으로 나열 후 8개 조회
    @Query(
            value = """
        SELECT 
            (
                SELECT i.image_url
                FROM products_images pi
                JOIN images i ON i.id = pi.product_image_id
                WHERE pi.product_id = p.id
                ORDER BY pi.created_at
                LIMIT 1
            ) AS imageUrl,
            s.vendor_name AS vendorName,
            p.title AS title,
            (
                SELECT ROUND(AVG(r.star)::numeric, 1)
                FROM reviews r
                WHERE r.product_id = p.id
            ) AS averageStar,
            (
                SELECT COUNT(*)
                FROM reviews r
                WHERE r.product_id = p.id
            ) AS reviewCount,
            p.price,
            p.is_discounted AS isDiscounted,
            p.discount_rate AS discountRate,
            p.created_at AS createdAt
        FROM products p
        JOIN sellers s ON s.user_id = p.seller_id
        WHERE p.is_discounted = true
        ORDER BY p.discount_rate DESC NULLS LAST
        LIMIT 8
    """,
            nativeQuery = true
    )
    List<MainProductProjection> findTop8ByOrderByDiscountRateDesc();

    // 베스트 셀러 - bestScore 계산하여 상위 8개 조회
    @Query(
            value = """
        WITH 
        sales_data AS (
            SELECT 
                p.id as product_id,
                COALESCE(SUM(oi.quantity), 0) as sales_quantity,
                COALESCE(SUM(oi.price * oi.quantity), 0) as total_revenue
            FROM products p
            LEFT JOIN order_items oi ON p.id = oi.product_id
            LEFT JOIN orders o ON oi.order_id = o.id
            WHERE (o.order_status IS NULL OR o.order_status IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED'))
            GROUP BY p.id
        ),
        review_data AS (
            SELECT 
                p.id as product_id,
                COALESCE(ROUND(AVG(r.star)::numeric, 1), 0.0) as avg_rating,
                COALESCE(COUNT(r.id), 0) as review_count
            FROM products p
            LEFT JOIN reviews r ON p.id = r.product_id
            GROUP BY p.id
        ),
        recent_orders AS (
            SELECT 
                p.id as product_id,
                COALESCE(COUNT(DISTINCT o.id), 0) as recent_order_count
            FROM products p
            LEFT JOIN order_items oi ON p.id = oi.product_id
            LEFT JOIN orders o ON oi.order_id = o.id
            WHERE o.created_at >= NOW() - INTERVAL '30 days'
            AND o.order_status IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED')
            GROUP BY p.id
        )
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
            s.vendor_name AS vendorName,
            p.title AS title,
            COALESCE(rd.avg_rating, 0.0) AS averageStar,
            COALESCE(rd.review_count, 0) AS reviewCount,
            p.price,
            p.is_discounted AS isDiscounted,
            p.discount_rate AS discountRate,
            p.created_at AS createdAt,
            (
                -- 판매량 정규화(0~100) * 0.4
                LEAST(100.0, COALESCE(sd.sales_quantity,0)/100.0*100.0) * 0.4 +
                -- 매출액 정규화(0~100) * 0.3
                LEAST(100.0, COALESCE(sd.total_revenue,0)/1000000.0*100.0) * 0.3 +
                -- 평점 정규화(0~100) * 0.15
                LEAST(100.0, COALESCE(rd.avg_rating,0)/5.0*100.0) * 0.15 +
                -- 리뷰수 정규화(0~100) * 0.1
                LEAST(100.0, COALESCE(rd.review_count,0)/50.0*100.0) * 0.1 +
                -- 최근30일주문수 정규화(0~100) * 0.05
                LEAST(100.0, COALESCE(ro.recent_order_count,0)/20.0*100.0) * 0.05
            ) AS bestScore
        FROM products p
        JOIN sellers s ON s.user_id = p.seller_id
        LEFT JOIN sales_data sd ON p.id = sd.product_id
        LEFT JOIN review_data rd ON p.id = rd.product_id
        LEFT JOIN recent_orders ro ON p.id = ro.product_id
        ORDER BY bestScore DESC NULLS LAST
        LIMIT 8
        """,
        nativeQuery = true
    )
    List<MainProductProjection> findTop8ByBestScoreDesc();


    @Modifying(clearAutomatically = true)
    @Query("""
    UPDATE Products p
    SET p.stock =: stock
    WHERE p.id =: id
    """)
    void updateStock(@Param("id") String id,
                     @Param("stock") int stock);

    @Query("""
    SELECT p
      FROM Products p
      JOIN p.seller s
      JOIN s.user u
     WHERE p.id           = :id
       AND u.provider     = :provider
       AND u.providerId   = :providerId
    """)
    Optional<Products> findProductsByIdAndProviderId(@Param("id") String id,
                                             @Param("provider") String provider,
                                             @Param("providerId") String providerId);
}