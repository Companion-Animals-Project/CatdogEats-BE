package com.team5.catdogeats.products.domain.dto;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.enums.AdjustmentType;
import com.team5.catdogeats.products.domain.mapping.InventoryAdjustments;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustmentRequestDTO(@NotBlank String id,
                                   @NotNull AdjustmentType type,
                                   @NotNull
                                   @Min(value = 1, message = "재고 조정 수량은 1 이상이어야 합니다.")
                                   int quantity,
                                   String note) {

    public static InventoryAdjustments toEntity(AdjustmentRequestDTO dto, Products products, Sellers sellers) {
        return InventoryAdjustments.builder()
                .products(products)
                .sellers(sellers)
                .adjustmentType(dto.type())
                .quantity(dto.quantity())
                .note(dto.note())
                .build();
    }
}
