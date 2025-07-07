package com.team5.catdogeats.reviews.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.pets.domain.dto.PetInfoResponseDto;
import com.team5.catdogeats.pets.domain.enums.Gender;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.dto.*;
import com.team5.catdogeats.reviews.mapper.ReviewMapper;
import com.team5.catdogeats.reviews.repository.ReviewClassificationLLMRepository;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.reviews.service.ReviewService;
import com.team5.catdogeats.storage.domain.dto.ReviewImageResponseDto;
import com.team5.catdogeats.storage.domain.mapping.ReviewsImages;
import com.team5.catdogeats.storage.repository.ReviewImageRepository;
import com.team5.catdogeats.storage.service.ReviewImageService;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final BuyerRepository buyerRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewImageService reviewImageService;
    private final ReviewMapper reviewMapper;
    private final ReviewClassificationLLMRepository reviewClassificationLLMRepository;

    @Override
    public String registerReview(UserPrincipal userPrincipal, ReviewCreateRequestDto dto) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        Products product = productRepository.findById(dto.productId())
                .orElseThrow(() -> new NoSuchElementException("해당 상품 정보를 찾을 수 없습니다."));

        Buyers buyer = Buyers.builder()
                .userId(buyerDTO.userId())
                .nameMaskingStatus(buyerDTO.nameMaskingStatus())
                .build();

        Reviews review = Reviews.fromDto(dto, buyer, product);

        return reviewRepository.save(review).getId();
    }

    @JpaTransactional(readOnly = true)
    @Override
    public Page<MyReviewResponseDto> getReviewsByBuyer(UserPrincipal userPrincipal, int page, int size) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        int offset = page * size;

        List<Object[]> flatRows = reviewRepository.findReviewsWithImagesAndProductByBuyerNative(buyerDTO.userId(), size, offset);

        // 리뷰ID별로 group by
        Map<String, MyReviewResponseDtoBuilder> builderMap = new LinkedHashMap<>();
        for (Object[] row : flatRows) {
            String reviewId = (String) row[0];
            String productName = (String) row[1];
            Double star = row[2] != null ? ((Number) row[2]).doubleValue() : null;
            String contents = (String) row[3];
            String updatedAt = row[4] != null ? row[4].toString() : null;
            String imageId = (String) row[5];
            String imageUrl = (String) row[6];

            MyReviewResponseDtoBuilder builder = builderMap.computeIfAbsent(reviewId, rid ->
                    new MyReviewResponseDtoBuilder(reviewId, productName, star, contents, updatedAt)
            );
            if (imageId != null && imageUrl != null) {
                builder.addImage(new ReviewImageResponseDto(imageId, imageUrl));
            }
        }

        long total = reviewRepository.countByBuyerId(buyerDTO.userId());

        List<MyReviewResponseDto> dtos = builderMap.values().stream()
                .map(MyReviewResponseDtoBuilder::build)
                .toList();

        return new PageImpl<>(dtos, PageRequest.of(page, size), total);
    }

    @JpaTransactional(readOnly = true)
    @Override
    public Page<ProductReviewResponseDto> getReviewsByProductNumber(Long productNumber, int page, int size) {
        Products product = productRepository.findByProductNumber(productNumber)
                .orElseThrow(() -> new NoSuchElementException("해당 상품 정보를 찾을 수 없습니다."));

        int offset = page * size;

        // 1. 리뷰 id만 먼저 page/size로 조회
        List<String> reviewIds = reviewMapper.findReviewIdsByProductNumber(productNumber, offset, size);
        if (reviewIds.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
        }

        // 2. 리뷰 id 리스트로 곱집합 row 조회
        List<ReviewMapper.FlatReviewRow> flatRows = reviewMapper.findReviewFlatRowsByIds(reviewIds);

        // 3. 기존 builder/grouping 코드 그대로
        Map<String, ProductReviewResponseDtoBuilder> reviewBuilders = new LinkedHashMap<>();
        for (ReviewMapper.FlatReviewRow row : flatRows) {
            ProductReviewResponseDtoBuilder builder = reviewBuilders.computeIfAbsent(row.reviewId, rid ->
                    new ProductReviewResponseDtoBuilder(
                            row.reviewId,
                            row.writerName,
                            row.nameMaskingStatus,
                            row.star,
                            row.contents,
                            row.updatedAt
                    )
            );
            if (row.petBreed != null && row.petAge != null && row.petGender != null) {
                builder.addPet(new PetInfoResponseDto(row.petBreed, row.petAge, Gender.valueOf(row.petGender)));
            }
            if (row.imageId != null && row.imageUrl != null) {
                builder.addImage(new ReviewImageResponseDto(row.imageId, row.imageUrl));
            }
        }
        long total = reviewMapper.countReviewsByProductNumber(productNumber);

        List<ProductReviewResponseDto> dtos = reviewBuilders.values().stream()
                .map(ProductReviewResponseDtoBuilder::build)
                .toList();

        return new PageImpl<>(dtos, PageRequest.of(page, size), total);
    }

    @JpaTransactional
    @Override
    public void updateReview(ReviewUpdateRequestDto dto) {
        Reviews review = reviewRepository.findById(dto.reviewId())
                .orElseThrow(() -> new NoSuchElementException("해당 리뷰를 찾을 수 없습니다."));

        review.updateFromDto(dto);
    }

    @JpaTransactional
    @Override
    public void deleteReview(ReviewDeleteRequestDto dto) {
        Reviews review = reviewRepository.findById(dto.reviewId())
                .orElseThrow(() -> new NoSuchElementException("해당 리뷰를 찾을 수 없습니다."));
        // 0. classification 결과 먼저 삭제
        reviewClassificationLLMRepository.deleteAllByReview(review);
        // 1. 리뷰와 연결된 모든 이미지 매핑 조회
        List<ReviewsImages> mappings = reviewImageRepository.findAllByReviewsId(dto.reviewId());
        // 2. 이미지 삭제 서비스 호출
        for (ReviewsImages mapping : mappings) {
            reviewImageService.deleteReviewImage(dto.reviewId(), mapping.getImages().getId());
        }
        reviewRepository.deleteById(dto.reviewId());
    }
}
