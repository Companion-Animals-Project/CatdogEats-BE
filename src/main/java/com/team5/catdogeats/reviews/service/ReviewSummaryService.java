package com.team5.catdogeats.reviews.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.team5.catdogeats.reviews.domain.dto.ReviewSummaryResponseDto;

public interface ReviewSummaryService {
    ReviewSummaryResponseDto getReviewSummaryByProductNumber(Long productNumber) throws JsonParseException;
}