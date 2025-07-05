package com.team5.catdogeats.carts.repository;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartRecommendationRepository extends JpaRepository<Products, String> {

    // 특정 카테고리 인기 상품 조회 (제외 상품 있을 때)
    @Query(value = """
        SELECT p.*, COALESCE(SUM(oi.quantity), 0) as orderCount
        FROM products p
        LEFT JOIN order_items oi ON oi.product_id = p.id
        LEFT JOIN orders o ON o.id = oi.order_id
        WHERE p.petcategory = :petCategory
          AND (o.order_status IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
          AND p.id NOT IN (:excludeProductIds)
        GROUP BY p.id, p.product_number, p.title, p.price, p.petcategory
        ORDER BY COALESCE(SUM(oi.quantity), 0) DESC, p.id ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Products> findTopPopularProductsByCategoryExcluding(
            @Param("petCategory") String petCategory,
            @Param("excludeProductIds") List<String> excludeProductIds,
            @Param("limit") int limit
    );

    // 특정 카테고리 인기 상품 조회 (제외 상품 없을 때)
    @Query(value = """
        SELECT p.*, COALESCE(SUM(oi.quantity), 0) as orderCount
        FROM products p
        LEFT JOIN order_items oi ON oi.product_id = p.id
        LEFT JOIN orders o ON o.id = oi.order_id
        WHERE p.petcategory = :petCategory
          AND (o.order_status IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
        GROUP BY p.id, p.product_number, p.title, p.price, p.petcategory
        ORDER BY COALESCE(SUM(oi.quantity), 0) DESC, p.id ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Products> findTopPopularProductsByCategory(
            @Param("petCategory") String petCategory,
            @Param("limit") int limit
    );

    // 전체 인기 상품 조회 (제외 상품 있을 때)
    @Query(value = """
        SELECT p.*, COALESCE(SUM(oi.quantity), 0) as orderCount
        FROM products p
        LEFT JOIN order_items oi ON oi.product_id = p.id
        LEFT JOIN orders o ON o.id = oi.order_id
        WHERE (o.order_status IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
          AND p.id NOT IN (:excludeProductIds)
        GROUP BY p.id, p.product_number, p.title, p.price, p.petcategory
        ORDER BY COALESCE(SUM(oi.quantity), 0) DESC, p.id ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Products> findTopPopularProductsExcluding(
            @Param("excludeProductIds") List<String> excludeProductIds,
            @Param("limit") int limit
    );

    // 전체 인기 상품 조회 (제외 상품 없을 때)
    @Query(value = """
        SELECT p.*, COALESCE(SUM(oi.quantity), 0) as orderCount
        FROM products p
        LEFT JOIN order_items oi ON oi.product_id = p.id
        LEFT JOIN orders o ON o.id = oi.order_id
        WHERE (o.order_status IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
        GROUP BY p.id, p.product_number, p.title, p.price, p.petcategory
        ORDER BY COALESCE(SUM(oi.quantity), 0) DESC, p.id ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Products> findTopPopularProductsAll(@Param("limit") int limit);
}