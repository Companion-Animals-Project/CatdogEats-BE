package com.team5.catdogeats.products.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.dto.*;
import com.team5.catdogeats.products.domain.enums.BuyerProductSortType;
import com.team5.catdogeats.products.domain.enums.MainProductSortType;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.exception.DuplicateProductNumberException;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.products.service.InventoryAdjustmentService;
import com.team5.catdogeats.products.service.ProductService;
import com.team5.catdogeats.reviews.repository.*;
import com.team5.catdogeats.storage.domain.mapping.ProductsImages;
import com.team5.catdogeats.storage.repository.ProductImageRepository;
import com.team5.catdogeats.storage.service.ProductImageService;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final SellersRepository sellerRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductImageService productImageService;
    private final InventoryAdjustmentService inventoryAdjustmentService;
    private final ReviewRepository reviewRepository;
    private final ReviewSummaryLLMRepository reviewSummaryLLMRepository;
    private final ReviewClassificationLLMCatHandmadeRepository reviewClassificationLLMCatHandmadeRepository;
    private final ReviewClassificationLLMCatFinishedRepository reviewClassificationLLMCatFinishedRepository;
    private final ReviewClassificationLLMDogHandmadeRepository reviewClassificationLLMDogHandmadeRepository;
    private final ReviewClassificationLLMDogFinishedRepository reviewClassificationLLMDogFinishedRepository;

    @Override
    public String registerProduct(UserPrincipal userPrincipal, ProductCreateRequestDto dto) {
        SellerDTO sellerDTO = sellerRepository.findSellerDtoByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));
        Sellers seller = SellerDTO.toEntity(sellerDTO);
        Long productNumber = getProductNumber();

        Products product = productRepository.save(Products.fromDto(dto, seller, productNumber));
        return product.getId();
    }


    @JpaTransactional
    @Override
    public void updateProduct(ProductUpdateRequestDto dto) {
        Products product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new NoSuchElementException("해당 아이템 정보를 찾을 수 없습니다."));

        product.updateFromDto(dto);
    }

    @JpaTransactional
    @Override
    public void deleteProduct(ProductDeleteRequestDto dto) {
        Products product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new NoSuchElementException("해당 아이템 정보를 찾을 수 없습니다."));

        reviewRepository.deleteAllByProduct(product);
        // 카테고리별 리뷰분류 결과 삭제
        PetCategory petCategory = product.getPetCategory();
        ProductCategory productCategory = product.getProductCategory();

        if (petCategory == PetCategory.CAT && productCategory == ProductCategory.HANDMADE) {
            reviewClassificationLLMCatHandmadeRepository.deleteAllByProduct(product);
        } else if (petCategory == PetCategory.CAT && productCategory == ProductCategory.FINISHED) {
            reviewClassificationLLMCatFinishedRepository.deleteAllByProduct(product);
        } else if (petCategory == PetCategory.DOG && productCategory == ProductCategory.HANDMADE) {
            reviewClassificationLLMDogHandmadeRepository.deleteAllByProduct(product);
        } else if (petCategory == PetCategory.DOG && productCategory == ProductCategory.FINISHED) {
            reviewClassificationLLMDogFinishedRepository.deleteAllByProduct(product);
        }
        // 리뷰요약 결과 삭제
        reviewSummaryLLMRepository.deleteAllByProduct(product);

        // 1. 리뷰와 연결된 모든 이미지 매핑 조회
        List<ProductsImages> mappings = productImageRepository.findAllByProductsId(dto.productId());
        // 2. 이미지 삭제 서비스 호출
        for (ProductsImages mapping : mappings) {
            productImageService.deleteProductImage(dto.productId(), mapping.getImages().getId());
        }

        productRepository.deleteById(dto.productId());
    }

    // 상품 조회 서비스 로직
    @Override
    public Page<ProductListProjection> getProductList(PetCategory petCategory, ProductCategory productCategory, BuyerProductSortType sortBy, Pageable pageable) {
        String petCategoryStr = petCategory != null ? petCategory.name() : null;
        String productCategoryStr = productCategory != null ? productCategory.name() : null;

        return switch (sortBy) {
            case PRICE ->
                    productRepository.findAllByOrderByDiscountedPriceDesc(petCategoryStr, productCategoryStr, pageable);
            case AVERAGE_STAR ->
                    productRepository.findAllByOrderByAverageStarDesc(petCategoryStr, productCategoryStr, pageable);
            default ->
                    productRepository.findAllByOrderByCreatedAtDesc(petCategoryStr, productCategoryStr, pageable);
        };
    }

    // 상품 상세 조회 서비스 로직
    @Override
    public ProductDetailResponseDto getProductDetail(Long productNumber) {

        ProductDetailProjection projection = productRepository.findProductDetailByProductNumber(productNumber);
        if (projection == null) throw new NoSuchElementException("해당 상품 정보를 찾을 수 없습니다.");


        return new ProductDetailResponseDto(
                projection.getTitle(),
                projection.getSubTitle(),
                projection.getProductInfo(),
                projection.getContents(),
                projection.getIsDiscounted() != null && projection.getIsDiscounted(),
                projection.getDiscountRate(),
                projection.getPrice(),
                projection.getImages(),
                projection.getVendorName(),
                projection.getAverageStar(),
                projection.getReviewCount() != null ? projection.getReviewCount() : 0
        );
    }

    @Override
    public List<MainProductResponseDto> getMainProducts(MainProductSortType type) {
        List<MainProductProjection> projections = switch (type) {
            case BEST -> productRepository.findTop8ByBestScoreDesc();
            case DISCOUNT -> productRepository.findTop8ByOrderByDiscountRateDesc();
            default -> productRepository.findTop8ByOrderByCreatedAtDesc();
        };
        return projections.stream()
                .map(p -> new MainProductResponseDto(
                        p.getImageUrl(),
                        p.getVendorName(),
                        p.getTitle(),
                        p.getAverageStar(),
                        p.getReviewCount() != null ? p.getReviewCount() : 0,
                        p.getPrice(),
                        p.getIsDiscounted(),
                        p.getDiscountRate(),
                        p.getCreatedAt()
                ))
                .toList();
    }


    private Long getProductNumber() {

        try {
            return generateProductNumber();
        } catch (DuplicateProductNumberException e) {
            log.warn("상품 번호 중복 발생, 1회 재시도");
            try {
                return generateProductNumber();
            } catch (DuplicateProductNumberException ex) {
                throw new IllegalStateException("상품 번호 생성 실패: 중복으로 인한 재시도 실패", ex);
            }
        }
    }

    /**
     * 고유한 상품 번호를 생성하는 메서드
     * (yyyyMMddHHmmss + 6자리 랜덤 숫자)
     */
    private Long generateProductNumber() {
        String timestamp = DateTimeFormatter.ofPattern("yyMMddHHmmss")
                .format(LocalDateTime.now());
        int randomNum = ThreadLocalRandom.current().nextInt(1000, 10000);
        Long productNumber = Long.parseLong(timestamp + randomNum);

        if (productRepository.existsByProductNumber(productNumber)) {
            throw new DuplicateProductNumberException("중복된 상품 번호: " + productNumber);
        }

        log.debug("상품 번호 생성 성공: {}", productNumber);
        return productNumber;
    }
}
