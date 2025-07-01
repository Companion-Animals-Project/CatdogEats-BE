package com.team5.catdogeats.carts.repository;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRecommendationRepository extends JpaRepository<Products, String> {

    // 특정 카테고리 인기 상품 조회 (제외 상품 있을 때)
    @Query("""
        SELECT DISTINCT p
        FROM Products p
        LEFT JOIN OrderItems oi ON oi.products.id = p.id
        LEFT JOIN Orders o ON o.id = oi.orders.id
        WHERE p.petCategory = :petCategory
          AND (o.orderStatus IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
          AND p.id NOT IN :excludeProductIds
        GROUP BY p.id, p.productNumber, p.title, p.price, p.petCategory
        ORDER BY COUNT(oi.id) DESC
        """)
    List<Products> findPopularProductsByCategoryExcluding(
            @Param("petCategory") PetCategory petCategory,
            @Param("excludeProductIds") List<String> excludeProductIds
    );

    // 특정 카테고리 인기 상품 조회 (제외 상품 없을 때)
    @Query("""
        SELECT DISTINCT p
        FROM Products p
        LEFT JOIN OrderItems oi ON oi.products.id = p.id
        LEFT JOIN Orders o ON o.id = oi.orders.id
        WHERE p.petCategory = :petCategory
          AND (o.orderStatus IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
        GROUP BY p.id, p.productNumber, p.title, p.price, p.petCategory
        ORDER BY COUNT(oi.id) DESC
        """)
    List<Products> findPopularProductsByCategory(
            @Param("petCategory") PetCategory petCategory
    );

    // 전체 인기 상품 조회 (제외 상품 있을 때)
    @Query("""
        SELECT DISTINCT p
        FROM Products p
        LEFT JOIN OrderItems oi ON oi.products.id = p.id
        LEFT JOIN Orders o ON o.id = oi.orders.id
        WHERE (o.orderStatus IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
          AND p.id NOT IN :excludeProductIds
        GROUP BY p.id, p.productNumber, p.title, p.price, p.petCategory
        ORDER BY COUNT(oi.id) DESC
        """)
    List<Products> findPopularProductsExcluding(
            @Param("excludeProductIds") List<String> excludeProductIds
    );

    // 전체 인기 상품 조회 (제외 상품 없을 때)
    @Query("""
        SELECT DISTINCT p
        FROM Products p
        LEFT JOIN OrderItems oi ON oi.products.id = p.id
        LEFT JOIN Orders o ON o.id = oi.orders.id
        WHERE (o.orderStatus IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
        GROUP BY p.id, p.productNumber, p.title, p.price, p.petCategory
        ORDER BY COUNT(oi.id) DESC
        """)
    List<Products> findPopularProductsAll();
}