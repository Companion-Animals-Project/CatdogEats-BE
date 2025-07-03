package com.team5.catdogeats.storage.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.storage.domain.Images;
import com.team5.catdogeats.storage.domain.dto.ProductImageUploadResponseDto;
import com.team5.catdogeats.storage.domain.mapping.ProductsImages;
import com.team5.catdogeats.storage.exception.ImageUploadException;
import com.team5.catdogeats.storage.repository.ImageRepository;
import com.team5.catdogeats.storage.repository.ProductImageRepository;
import com.team5.catdogeats.storage.service.ObjectStorageService;
import com.team5.catdogeats.storage.service.ProductImageDBHelper;
import com.team5.catdogeats.storage.service.ProductImageService;
import com.team5.catdogeats.storage.util.ImageValidationUtil;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductImageSerivceImpl implements ProductImageService {

    private final ObjectStorageService objectStorageService;
    private final ImageRepository imageRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final ImageValidationUtil imageValidationUtil;
    private final SellersRepository sellerRepository;
    private final ProductImageDBHelper productImageDBHelper;

    @JpaTransactional
    @Override
    public List<ProductImageUploadResponseDto> uploadProductImage(UserPrincipal userPrincipal, String productId, List<MultipartFile> images) throws IOException {

        SellerDTO sellerDTO = sellerRepository.findSellerDtoByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        if (images.size() > 10) {
            throw new IllegalArgumentException("이미지는 한 번에 최대 10개까지만 업로드할 수 있습니다.");
        }

        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("해당 아이템 정보를 찾을 수 없습니다."));

        List<ProductImageUploadResponseDto> result = new ArrayList<>();

        for (MultipartFile file : images) {
            String s3Url;
            Images savedImage;

            imageValidationUtil.validateImageFile(file);

            // 동일한 이름을 가진 이미지 파일 덮어쓰기 방지
            String uniqueKey = generateUniqueFileName(file.getOriginalFilename(), productId);

            // S3 업로드
            s3Url = objectStorageService.uploadImage(
                    uniqueKey,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );

            // Images 저장 (retryable)
            try {
                savedImage = productImageDBHelper.storeImageToDbWithRetry(s3Url);
            } catch (ImageUploadException e) {
                // S3에 올라간 이미지는 삭제 시도
                try {
                    String fileKey = s3Url.substring(s3Url.lastIndexOf("/") + 1);
                    log.warn("[uploadProductImage] DB(Images) 저장 최종 실패. S3에서 파일({}) 롤백(삭제) 시도!", fileKey);
                    objectStorageService.deleteImage(fileKey);
                } catch (Exception ignore) {
                    log.error("[uploadProductImage] S3 롤백 중 추가 에러: {}", ignore.getMessage());
                }
                log.error("[uploadProductImage] Images 저장 실패 후 최종 롤백 완료.");
                throw e;
            }

            // products_images 매핑 저장 (retryable)
            try {
                productImageDBHelper.storeProductImageMappingWithRetry(product, savedImage);
            } catch (ImageUploadException e) {
                // Images, S3 모두 롤백 시도
                try {
                    log.warn("[uploadProductImage] products_images 매핑 최종 실패. Images({}) 롤백(삭제) 시도!", savedImage.getId());
                    imageRepository.delete(savedImage);
                } catch (Exception ignore) {
                    log.error("[uploadProductImage] Images 롤백 중 추가 에러: {}", ignore.getMessage());
                }
                try {
                    String fileKey = s3Url.substring(s3Url.lastIndexOf("/") + 1);
                    log.warn("[uploadProductImage] products_images 매핑 최종 실패. S3({}) 롤백(삭제) 시도!", fileKey);
                    objectStorageService.deleteImage(fileKey);
                } catch (Exception ignore) {
                    log.error("[uploadProductImage] S3 롤백 중 추가 에러: {}", ignore.getMessage());
                }
                log.error("[uploadProductImage] products_images 매핑 저장 실패 후 최종 롤백 완료.");
                throw e;
            }


            result.add(new ProductImageUploadResponseDto(savedImage.getId(), s3Url));
        }

        return result;
    }

    @JpaTransactional
    @Override
    public List<ProductImageUploadResponseDto> updateProductImage(UserPrincipal userPrincipal, String productId, List<String> oldImageIds, List<MultipartFile> images) throws IOException {
        // 1. 기존 이미지/매핑/S3에 있는 이미지들 중 골라서 삭제
        for (String oldImageId : oldImageIds) {
            this.deleteProductImage(productId, oldImageId);
        }

        // 2. 새 이미지 업로드/매핑
        return this.uploadProductImage(userPrincipal, productId, images);

    }

    @JpaTransactional
    @Override
    public void deleteProductImage(String productId, String imageId) {
        ProductsImages mapping = productImageRepository.findByProductsIdAndImagesId(productId, imageId)
                .orElseThrow(() -> new NoSuchElementException("해당 매핑 데이터 없음"));

        // S3에서 이미지 파일 삭제 (images/{파일명})
        String imageUrl = mapping.getImages().getImageUrl();
        String fileKey = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        objectStorageService.deleteImage(fileKey);

        // 매핑, 이미지 DB 삭제
        productImageRepository.delete(mapping);
        imageRepository.deleteById(imageId);
    }

    /**
     * 고유한 파일명 생성
     * 형식: product_{productId}_{UUID}.{확장자}
     */
    private String generateUniqueFileName(String originalFileName, String productId) {
        String extension = imageValidationUtil.getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");

        String shortProductId = productId.length() > 8 ? productId.substring(0, 8) : productId;

        // 처음 8자리만 사용 (너무 길어지는 것 방지)
        return String.format("product_%s_%s.%s", shortProductId, uuid, extension);
    }
}
