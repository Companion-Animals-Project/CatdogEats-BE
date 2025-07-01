package com.team5.catdogeats.products.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductCreateRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductDeleteRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductListProjection;
import com.team5.catdogeats.products.domain.dto.ProductUpdateRequestDto;
import com.team5.catdogeats.products.domain.enums.BuyerProductSortType;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    String registerProduct(UserPrincipal userPrincipal, ProductCreateRequestDto dto);

    void updateProduct(ProductUpdateRequestDto dto);

    void deleteProduct(ProductDeleteRequestDto dto);

    Page<ProductListProjection> getProductList (
            PetCategory petCategory,
            ProductCategory productCategory,
            BuyerProductSortType sortBy,
            Pageable pageable
    );
}
