package com.team5.catdogeats.products.domain.dto;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;


/**
 * 판매자 스토어 페이지에서 사용할 상품 정보 DTO
 * Products 도메인에서 Users 도메인으로 데이터 전달용
 */
public record ProductStoreInfoDTO(
        String productId,
        Long productNumber,
        String title,
        Long price,
        boolean isDiscounted,
        Double discountRate,
        String mainImageUrl,
        PetCategory petCategory,
        ProductCategory productCategory,
        Integer stock,
        Integer safetyStock,
        Double avgRating,
        Long reviewCount,
        Double bestScore
) {

    /**
     * 재고 상태를 계산하여 반환하는 메서드
     * Products 엔티티의 getStockStatus() 로직과 동일
     */
    public StockStatus getStockStatus() {
        if (stock == null || stock <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (safetyStock != null && stock <= safetyStock) {
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.IN_STOCK;
    }
}