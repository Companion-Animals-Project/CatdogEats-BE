package com.team5.catdogeats.carts.repository;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CartRecommendationRepository extends JpaRepository<Products, String> {

    // 특정 카테고리 인기 상품 조회 (제외 상품 있을 때)
    @Query("""
        SELECT p, COALESCE(SUM(oi.quantity), 0) as orderCount
        FROM Products p
        LEFT JOIN OrderItems oi ON oi.products.id = p.id
        LEFT JOIN Orders o ON o.id = oi.orders.id
        WHERE p.petCategory = :petCategory
          AND (o.orderStatus IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
          AND p.id NOT IN :excludeProductIds
        GROUP BY p.id, p.productNumber, p.title, p.price, p.petCategory
        ORDER BY COALESCE(SUM(oi.quantity), 0) DESC, p.id ASC
        """)
    List<Object[]> findPopularProductsByCategoryExcludingWithCount(
            @Param("petCategory") PetCategory petCategory,
            @Param("excludeProductIds") List<String> excludeProductIds
    );

    // 특정 카테고리 인기 상품 조회 (제외 상품 없을 때)
    @Query("""
        SELECT p, COALESCE(SUM(oi.quantity), 0) as orderCount
        FROM Products p
        LEFT JOIN OrderItems oi ON oi.products.id = p.id
        LEFT JOIN Orders o ON o.id = oi.orders.id
        WHERE p.petCategory = :petCategory
          AND (o.orderStatus IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
        GROUP BY p.id, p.productNumber, p.title, p.price, p.petCategory
        ORDER BY COALESCE(SUM(oi.quantity), 0) DESC, p.id ASC
        """)
    List<Object[]> findPopularProductsByCategoryWithCount(
            @Param("petCategory") PetCategory petCategory
    );

    // 전체 인기 상품 조회 (제외 상품 있을 때)
    @Query("""
        SELECT p, COALESCE(SUM(oi.quantity), 0) as orderCount
        FROM Products p
        LEFT JOIN OrderItems oi ON oi.products.id = p.id
        LEFT JOIN Orders o ON o.id = oi.orders.id
        WHERE (o.orderStatus IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
          AND p.id NOT IN :excludeProductIds
        GROUP BY p.id, p.productNumber, p.title, p.price, p.petCategory
        ORDER BY COALESCE(SUM(oi.quantity), 0) DESC, p.id ASC
        """)
    List<Object[]> findPopularProductsExcludingWithCount(
            @Param("excludeProductIds") List<String> excludeProductIds
    );

    // 전체 인기 상품 조회 (제외 상품 없을 때)
    @Query("""
        SELECT p, COALESCE(SUM(oi.quantity), 0) as orderCount
        FROM Products p
        LEFT JOIN OrderItems oi ON oi.products.id = p.id
        LEFT JOIN Orders o ON o.id = oi.orders.id
        WHERE (o.orderStatus IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
        GROUP BY p.id, p.productNumber, p.title, p.price, p.petCategory
        ORDER BY COALESCE(SUM(oi.quantity), 0) DESC, p.id ASC
        """)
    List<Object[]> findPopularProductsAllWithCount();

    // 기존 메서드들 (서비스에서 wrapper로 사용)
    default List<Products> findPopularProductsByCategoryExcluding(PetCategory petCategory, List<String> excludeProductIds) {
        return findPopularProductsByCategoryExcludingWithCount(petCategory, excludeProductIds)
                .stream()
                .map(result -> (Products) result[0])
                .toList();
    }

    default List<Products> findPopularProductsByCategory(PetCategory petCategory) {
        return findPopularProductsByCategoryWithCount(petCategory)
                .stream()
                .map(result -> (Products) result[0])
                .toList();
    }

    default List<Products> findPopularProductsExcluding(List<String> excludeProductIds) {
        return findPopularProductsExcludingWithCount(excludeProductIds)
                .stream()
                .map(result -> (Products) result[0])
                .toList();
    }

    default List<Products> findPopularProductsAll() {
        return findPopularProductsAllWithCount()
                .stream()
                .map(result -> (Products) result[0])
                .toList();
    }
}