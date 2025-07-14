package com.team5.catdogeats.reviews.service.impl;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.domain.dto.ReviewSummaryResponseDto;
import com.team5.catdogeats.reviews.domain.mapping.ReviewsSummaryLLM;
import com.team5.catdogeats.reviews.repository.ReviewSummaryLLMRepository;
import com.team5.catdogeats.reviews.service.ReviewSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewSummaryServiceImpl implements ReviewSummaryService {
    private final ProductRepository productRepository;
    private final ReviewSummaryLLMRepository reviewSummaryLLMRepository;
    private final ObjectMapper objectMapper;

    @Override
    @JpaTransactional (readOnly = true)
    public ReviewSummaryResponseDto getReviewSummaryByProductNumber(Long productNumber) throws JsonParseException {
        Products product = productRepository.findByProductNumber(productNumber)
                .orElseThrow(() -> new NoSuchElementException("해당 상품을 찾을 수 없습니다."));

        ReviewsSummaryLLM latestSummary = reviewSummaryLLMRepository.findTopByProductIdOrderByCreatedAtDesc(product.getId());
        if (latestSummary == null)
            throw new NoSuchElementException("해당 상품에 대한 리뷰 요약이 존재하지 않습니다.");

        String positiveSummary = latestSummary.getPositiveReview();
        List<String> positiveMainPoints = toList(latestSummary.getPositiveMainPoints());
        List<String> positiveKeywords = toList(latestSummary.getPositiveKeyword());
        String negativeSummary = latestSummary.getNegativeReview();
        List<String> negativeMainPoints = toList(latestSummary.getNegativeMainPoints());
        List<String> negativeKeywords = toList(latestSummary.getNegativeKeyword());

        return new ReviewSummaryResponseDto(
                product.getTitle(),
                positiveSummary,
                positiveMainPoints,
                positiveKeywords,
                negativeSummary,
                negativeMainPoints,
                negativeKeywords
        );
    }

    private List<String> toList(String jsonArrayString) throws JsonParseException {
        try {
            return objectMapper.readValue(jsonArrayString, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new JsonParseException("리뷰 요약 결과 JSON 파싱 중 에러 발생");
        }
    }
}
