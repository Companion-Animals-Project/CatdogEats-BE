package com.team5.catdogeats.reviews.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.products.domain.dto.ProductDeliveredResponseDto;
import com.team5.catdogeats.reviews.domain.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {
    String registerReview(UserPrincipal userPrincipal, ReviewCreateRequestDto dto);

    Page<MyReviewResponseDto> getReviewsByBuyer(UserPrincipal userPrincipal, int page, int size);

    Page<ProductReviewResponseDto> getReviewsByProductNumber(Long productNumber, int page, int size);

    void updateReview(ReviewUpdateRequestDto dto);

    void deleteReview(ReviewDeleteRequestDto dto);

    Page<ProductDeliveredResponseDto> getDeliveredProducts(UserPrincipal userPrincipal, Pageable pageable);
}
