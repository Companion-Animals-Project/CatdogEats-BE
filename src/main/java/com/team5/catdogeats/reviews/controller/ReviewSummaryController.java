package com.team5.catdogeats.reviews.controller;

import com.fasterxml.jackson.core.JsonParseException;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.reviews.domain.dto.ReviewSummaryResponseDto;
import com.team5.catdogeats.reviews.service.ReviewSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/buyers/reviews")
public class ReviewSummaryController {
    private final ReviewSummaryService reviewSummaryService;

    @GetMapping("/{productNumber}")
    public ResponseEntity<APIResponse<ReviewSummaryResponseDto>> getReviewSummary(@PathVariable Long productNumber) {
        try {
            ReviewSummaryResponseDto summary = reviewSummaryService.getReviewSummaryByProductNumber(productNumber);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, summary));
        } catch (JsonParseException e) {
            return ResponseEntity
                    .status(ResponseCode.JSON_PARSING_FAIL.getStatus())
                    .body(APIResponse.error(ResponseCode.JSON_PARSING_FAIL, e.getMessage()));
        } catch (NoSuchElementException e) {
            // 상품 없거나, 검증된 리뷰 및 리뷰 요약 없을 때
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }
}
