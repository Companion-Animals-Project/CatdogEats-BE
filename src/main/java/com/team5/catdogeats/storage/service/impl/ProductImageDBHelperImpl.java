package com.team5.catdogeats.storage.service.impl;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.storage.domain.Images;
import com.team5.catdogeats.storage.domain.mapping.ProductsImages;
import com.team5.catdogeats.storage.exception.ImageUploadException;
import com.team5.catdogeats.storage.repository.ImageRepository;
import com.team5.catdogeats.storage.repository.ProductImageRepository;
import com.team5.catdogeats.storage.service.ProductImageDBHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageDBHelperImpl implements ProductImageDBHelper {
    private final ImageRepository imageRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Retryable(
            retryFor = { Exception.class },
            backoff = @Backoff(delay = 1000)
    ) // retry 3회 시도
    public Images storeImageToDbWithRetry(String imageUrl) {
        log.info("[storeImageToDbWithRetry] Called. imageUrl={}", imageUrl);

        Images image = Images.builder().imageUrl(imageUrl).build();
        return imageRepository.save(image);
    }

    // retry 모두 실패 시, 수행
    @Recover
    public Images recoverStoreImageToDb(Exception e, String imageUrl) {
        log.error("[storeImageToDbWithRetry][Recover] 모든 재시도 실패! imageUrl={}, error={}", imageUrl, e.getMessage());
        throw new ImageUploadException("DB(Images) 저장 최종 실패: " + e.getMessage());
    }

    @Override
    @Retryable(
            retryFor = { Exception.class },
            backoff = @Backoff(delay = 1000)
    )
    public void storeProductImageMappingWithRetry(Products product, Images image) {
        log.info("[storeProductImageMappingWithRetry] Called. productId={}, imageId={}",
                product.getId(), image.getId());

        ProductsImages productImages = ProductsImages.builder()
                .products(product)
                .images(image)
                .build();
        productImageRepository.save(productImages);
    }

    @Recover
    public void recoverStoreProductImageMapping(Exception e, Products product, Images image) {
        log.error("[storeProductImageMappingWithRetry][Recover] 모든 재시도 실패! productId={}, imageId={}, error={}",
                product.getId(), image.getId(), e.getMessage());
        throw new ImageUploadException("DB(products_images) 매핑 최종 실패: " + e.getMessage());
    }
}
