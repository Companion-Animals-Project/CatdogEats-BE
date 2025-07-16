package com.team5.catdogeats.products.domain.dto;

import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import jakarta.validation.constraints.*;

public record ProductCreateRequestDto(
        @NotBlank(message = "상품명은 필수입니다.")
        String title,

        @NotBlank(message = "상품 부제는 필수입니다.")
        String subTitle,

        @NotBlank(message = "상품 정보는 필수입니다.")
        String productInfo,

        @NotBlank(message = "상품 설명은 필수입니다.")
        String contents,

        @NotNull(message = "반려동물 카테고리는 필수입니다.")
        PetCategory petCategory,

        @NotNull(message = "상품 카테고리는 필수입니다.")
        ProductCategory productCategory,

        @NotNull(message = "할인 적용 여부는 필수입니다.")
        Boolean isDiscounted,

        @Min(value = 0, message = "할인율은 0 이상이어야 합니다.")
        @Max(value = 100, message = "할인율은 100을 초과할 수 없습니다.")
        Short discountRate,

        @NotNull(message = "가격은 필수입니다.")
        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        Long price,

        @NotNull(message = "리드타임은 필수입니다.")
        @Min(value = 0, message = "리드타임은 0 이상이어야 합니다.")
        Short leadTime,

        @NotNull(message = "재고는 필수입니다.")
        @PositiveOrZero(message = "재고는 0 이상이어야 합니다.")
        Integer stock
) {
}
