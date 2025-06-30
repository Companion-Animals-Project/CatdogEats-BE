package com.team5.catdogeats.users.domain.dto;

import com.team5.catdogeats.global.dto.PageResponseDto;
import com.team5.catdogeats.products.domain.dto.MyProductResponseDto;
import com.team5.catdogeats.reviews.domain.dto.SellerReviewSummaryResponseDto;

public record SellerProductListWithReviewSummaryResponseDto(
        PageResponseDto<MyProductResponseDto> products, // 상품 목록
        SellerReviewSummaryResponseDto reviewSummary    // 리뷰 통계
) {
}
