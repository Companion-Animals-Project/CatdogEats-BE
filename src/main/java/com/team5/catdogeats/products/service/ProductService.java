package com.team5.catdogeats.products.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.*;
import com.team5.catdogeats.products.domain.enums.BuyerProductSortType;
import com.team5.catdogeats.products.domain.enums.MainProductSortType;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

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

    ProductDetailResponseDto getProductDetail(Long productNumber);

    List<MainProductResponseDto> getMainProducts(MainProductSortType type);

    Page<SellerProductListProjection> getSellerProductList(UserPrincipal userPrincipal, int page, int size);
}
