package com.team5.catdogeats.reviews.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.team5.catdogeats.reviews.domain.dto.ReviewSummaryResponseDto;

public interface ReviewSummaryService {
    ReviewSummaryResponseDto summarizeReviewsByProductNumber(Long productNumber, boolean forceRefresh) throws JsonProcessingException;
}