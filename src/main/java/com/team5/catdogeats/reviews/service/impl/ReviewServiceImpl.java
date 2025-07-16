package com.team5.catdogeats.reviews.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.repository.OrderItemRepository;
import com.team5.catdogeats.pets.domain.dto.PetInfoResponseDto;
import com.team5.catdogeats.pets.domain.enums.Gender;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.dto.ProductDeliveredResponseDto;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.dto.*;
import com.team5.catdogeats.reviews.mapper.ReviewMapper;
import com.team5.catdogeats.reviews.repository.*;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
    private final OrderItemRepository orderItemRepository;
    private final ReviewImageService reviewImageService;
    private final ReviewMapper reviewMapper;
    private final ReviewClassificationLLMCatHandmadeRepository reviewClassificationLLMCatHandmadeRepository;
    private final ReviewClassificationLLMCatFinishedRepository reviewClassificationLLMCatFinishedRepository;
    private final ReviewClassificationLLMDogHandmadeRepository reviewClassificationLLMDogHandmadeRepository;
    private final ReviewClassificationLLMDogFinishedRepository reviewClassificationLLMDogFinishedRepository;

    @Override
    public String registerReview(UserPrincipal userPrincipal, ReviewCreateRequestDto dto) {
        // Buyer의 PK(UUID) 사용
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        String userId = buyerDTO.userId(); // PK(UUID) 반드시 사용!
        String productId = dto.productId();

        Products product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("해당 상품 정보를 찾을 수 없습니다."));

        // 3. 배송완료 상태인 주문 내역에 해당 상품이 있는지 검증
        boolean hasDeliveredOrder = orderItemRepository.existsByOrders_Buyers_User_IdAndProducts_IdAndOrders_OrderStatus(
                userId, productId, OrderStatus.DELIVERED
        );
        if (!hasDeliveredOrder) {
            throw new IllegalStateException("배송 완료된 상품에 대해서만 리뷰 작성이 가능합니다.");
        }

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
        Products product = review.getProduct(); // 리뷰에 연결된 상품
        PetCategory petCategory = product.getPetCategory();
        ProductCategory productCategory = product.getProductCategory();

        // 0. classification 결과 먼저 삭제
        if (petCategory == PetCategory.CAT && productCategory == ProductCategory.HANDMADE) {
            reviewClassificationLLMCatHandmadeRepository.deleteAllByReview(review);
        } else if (petCategory == PetCategory.CAT && productCategory == ProductCategory.FINISHED) {
            reviewClassificationLLMCatFinishedRepository.deleteAllByReview(review);
        } else if (petCategory == PetCategory.DOG && productCategory == ProductCategory.HANDMADE) {
            reviewClassificationLLMDogHandmadeRepository.deleteAllByReview(review);
        } else if (petCategory == PetCategory.DOG && productCategory == ProductCategory.FINISHED) {
            reviewClassificationLLMDogFinishedRepository.deleteAllByReview(review);
        }
        // 1. 리뷰와 연결된 모든 이미지 매핑 조회
        List<ReviewsImages> mappings = reviewImageRepository.findAllByReviewsId(dto.reviewId());
        // 2. 이미지 삭제 서비스 호출
        for (ReviewsImages mapping : mappings) {
            reviewImageService.deleteReviewImage(dto.reviewId(), mapping.getImages().getId());
        }
        reviewRepository.deleteById(dto.reviewId());
    }

    @Override
    public Page<ProductDeliveredResponseDto> getDeliveredProducts(UserPrincipal userPrincipal, Pageable pageable) {
        // 1. Provider/ProviderId로 Buyer PK(UUID) 조회
        String buyerUuid = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .map(BuyerDTO::userId)
                .orElseThrow(() -> new NoSuchElementException("구매자 정보가 없습니다."));

        // 2. buyerUuid 쿼리를 사용
        Page<Object[]> page = orderItemRepository.findDeliveredProductsByUserId(buyerUuid, pageable);

        return page.map(row -> {
            String productId = (String) row[0];
            String productImage = (String) row[1];
            String productName = (String) row[2];
            Object deliveredAtRaw = row[3];
            ZonedDateTime deliveredAt = null;

            if (deliveredAtRaw != null) {
                if (deliveredAtRaw instanceof java.sql.Timestamp ts) {
                    deliveredAt = ts.toLocalDateTime().atZone(java.time.ZoneId.systemDefault());
                } else if (deliveredAtRaw instanceof java.time.Instant instant) {
                    deliveredAt = instant.atZone(java.time.ZoneId.systemDefault());
                } else if (deliveredAtRaw instanceof java.time.LocalDateTime ldt) {
                    deliveredAt = ldt.atZone(java.time.ZoneId.systemDefault());
                } else if (deliveredAtRaw instanceof java.util.Date date) {
                    deliveredAt = date.toInstant().atZone(java.time.ZoneId.systemDefault());
                } else {
                    throw new IllegalStateException("지원하지 않는 날짜 타입: " + deliveredAtRaw.getClass().getName());
                }
            }

            return new ProductDeliveredResponseDto(
                    productId,
                    productImage,
                    productName,
                    deliveredAt
            );
        });
    }
}
