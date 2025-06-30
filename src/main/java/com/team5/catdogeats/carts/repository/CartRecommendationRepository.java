package com.team5.catdogeats.carts.repository;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 장바구니 추천 상품 조회를 위한 Repository
 * 단순화된 버전으로 Products 엔티티를 직접 반환
 */
@Repository
public interface CartRecommendationRepository extends JpaRepository<Products, String> {

    /**
     * 특정 카테고리 인기 상품 조회 (강아지 또는 고양이 전용 장바구니용)
     * - 해당 카테고리에서 가장 많이 구매된 상품 순으로 정렬
     * - 장바구니에 이미 담긴 상품들은 제외
     *
     * @param petCategory 추천할 반려동물 카테고리
     * @param excludeProductIds 제외할 상품 ID 목록 (장바구니에 이미 담긴 상품들)
     * @return 추천 상품 목록 (4개 고정)
     */
    @Query("""
        SELECT DISTINCT p
        FROM Products p
        LEFT JOIN OrderItems oi ON oi.products.id = p.id
        LEFT JOIN Orders o ON o.id = oi.orders.id
        WHERE p.petCategory = :petCategory
          AND (o.orderStatus IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
          AND (:excludeProductIds IS NULL OR SIZE(:excludeProductIds) = 0 OR p.id NOT IN :excludeProductIds)
        GROUP BY p.id, p.productNumber, p.title, p.price, p.petCategory
        ORDER BY COUNT(oi.id) DESC
        """)
    List<Products> findPopularProductsByCategory(
            @Param("petCategory") PetCategory petCategory,
            @Param("excludeProductIds") List<String> excludeProductIds
    );

    /**
     * 전체 인기 상품 조회 (혼합 장바구니용)
     * - 카테고리 구분 없이 전체 상품에서 가장 많이 구매된 상품들
     * - 장바구니에 이미 담긴 상품들은 제외
     *
     * @param excludeProductIds 제외할 상품 ID 목록
     * @return 추천 상품 목록 (4개 고정)
     */
    @Query("""
        SELECT DISTINCT p
        FROM Products p
        LEFT JOIN OrderItems oi ON oi.products.id = p.id
        LEFT JOIN Orders o ON o.id = oi.orders.id
        WHERE (o.orderStatus IN ('PAYMENT_COMPLETED', 'PREPARING', 'READY_FOR_SHIPMENT', 'IN_DELIVERY', 'DELIVERED') OR o.id IS NULL)
          AND (:excludeProductIds IS NULL OR SIZE(:excludeProductIds) = 0 OR p.id NOT IN :excludeProductIds)
        GROUP BY p.id, p.productNumber, p.title, p.price, p.petCategory
        ORDER BY COUNT(oi.id) DESC
        """)
    List<Products> findPopularProductsAll(
            @Param("excludeProductIds") List<String> excludeProductIds
    );
}