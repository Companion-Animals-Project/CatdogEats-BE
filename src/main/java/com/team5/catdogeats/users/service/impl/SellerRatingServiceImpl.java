package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.products.domain.dto.MyProductResponseDto;
import com.team5.catdogeats.products.domain.enums.SellerProductSortType;
import com.team5.catdogeats.products.mapper.ProductMapper;
import com.team5.catdogeats.reviews.domain.dto.SellerReviewSummaryResponseDto;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.service.SellerRatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SellerRatingServiceImpl implements SellerRatingService {
    private final SellersRepository sellerRepository;
    private final ReviewRepository reviewRepository;
    private final ProductMapper productMapper;

    @Override
    public Page<MyProductResponseDto> getProductsBySeller(UserPrincipal userPrincipal, int page, int size, SellerProductSortType sortType) {
        SellerDTO sellerDTO = sellerRepository.findSellerDtoByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        String orderBy = switch (sortType) {
            case STAR -> "averageStar DESC";
            case REVIEW -> "reviewCount DESC";
            default -> "productId DESC"; // 최신순
        };

        long total = productMapper.countProductsBySellerId(sellerDTO.userId());
        int offset = page * size;

        List<MyProductResponseDto> dtos = productMapper.findProductSummariesBySellerId(
                sellerDTO.userId(), orderBy, offset, size
        );

        // 평균 별점 소수점 1자리 반올림
        List<MyProductResponseDto> fixedDtos = dtos.stream()
                .map(dto -> new MyProductResponseDto(
                        dto.productId(),
                        dto.productName(),
                        dto.reviewCount(),
                        dto.averageStar() != null ? Math.round(dto.averageStar() * 10) / 10.0 : 0.0,
                        dto.imageId(),
                        dto.imageUrl()
                ))
                .toList();

        return new PageImpl<>(fixedDtos, PageRequest.of(page, size), total);
    }


    @Override
    public SellerReviewSummaryResponseDto getSellerReviewSummary(UserPrincipal userPrincipal) {
        SellerDTO sellerDTO = sellerRepository.findSellerDtoByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("해당 유저 정보를 찾을 수 없습니다."));

        // 전체 평균, 전체 개수
        List<Object[]> avgCountList = reviewRepository.findAvgAndCountBySellerId(sellerDTO.userId());
        Object[] avgCount = avgCountList.isEmpty() ? new Object[]{0.0, 0L} : avgCountList.get(0);
        double avgStar = avgCount[0] != null ? ((Number) avgCount[0]).doubleValue() : 0.0;
        long totalCount = avgCount[1] != null ? ((Number) avgCount[1]).longValue() : 0L;

        // 구간별 개수
        Map<Integer, Long> starGroupCount = new HashMap<>();
        // 0~5점대 기본값 0으로 초기화
        for (int i = 0; i <= 5; i++) starGroupCount.put(i, 0L);

        for (Object[] groupRow : reviewRepository.findGroupStarCountBySellerId(sellerDTO.userId())) {
            Integer group = groupRow[0] != null ? ((Number) groupRow[0]).intValue() : null;
            Long count = groupRow[1] != null ? ((Number) groupRow[1]).longValue() : null;
            if (group != null && count != null) {
                starGroupCount.put(group, count);
            }
        }

        // 소수점 한 자리로 평균 반올림
        avgStar = Math.round(avgStar * 10) / 10.0;

        return new SellerReviewSummaryResponseDto(avgStar, totalCount, starGroupCount);
    }
}
