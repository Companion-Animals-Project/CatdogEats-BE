package com.team5.catdogeats.reviews.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.dto.PageResponseDto;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.products.domain.dto.ProductDeliveredResponseDto;
import com.team5.catdogeats.reviews.domain.dto.*;
import com.team5.catdogeats.reviews.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/v1/buyers/reviews")
@RequiredArgsConstructor
@Tag(name = "Review", description = "리뷰 관련 API")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 등록", description = "구매자가 상품에 대한 리뷰를 등록합니다.")
    @PostMapping
    public ResponseEntity<APIResponse<Void>> createReview(@AuthenticationPrincipal UserPrincipal userPrincipal, @RequestBody @Valid @Parameter(description = "등록할 리뷰 정보", required = true) ReviewCreateRequestDto dto) {
        try {
            String reviewId = reviewService.registerReview(userPrincipal, dto);
            return ResponseEntity
                    .created(URI.create("/v1/buyers/reviews/" + reviewId))
                    .body(APIResponse.success(ResponseCode.CREATED));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "나의 배송 완료된 상품 목록들", description = "로그인한 구매자가 직접 구매하여 배송 완료된 상품 목록들 조회합니다.")
    @GetMapping("/delivered-products")
    public ResponseEntity<APIResponse<Page<ProductDeliveredResponseDto>>> getDeliveredProducts(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "deliveredAt"));
            Page<ProductDeliveredResponseDto> result = reviewService.getDeliveredProducts(userPrincipal, pageable);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, result));
        } catch (IllegalStateException e) {
            // 비즈니스 위반(주로 BAD_REQUEST)로 응답
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        }catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "내가 작성한 리뷰 목록 조회 (페이징)", description = "로그인한 구매자가 직접 작성한 리뷰 목록을 조회합니다.")
    @GetMapping("/list")
    public ResponseEntity<APIResponse<PageResponseDto<MyReviewResponseDto>>> getReviewsByBuyer(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<MyReviewResponseDto> reviews = reviewService.getReviewsByBuyer(userPrincipal, page, size);

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, PageResponseDto.from(reviews)));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "상품에 대한 리뷰 목록 조회 (페이징)", description = "특정 상품에 대한 모든 리뷰 목록을 조회합니다.")
    @GetMapping("/{productNumber}/list")
    public ResponseEntity<APIResponse<PageResponseDto<ProductReviewResponseDto>>> getReviewsByProduct(
            @Parameter(description = "조회할 상품 Number", required = true)
            @PathVariable Long productNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Page<ProductReviewResponseDto> reviews = reviewService.getReviewsByProductNumber(productNumber, page, size);

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, PageResponseDto.from(reviews)));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "리뷰 수정", description = "구매자가 자신의 리뷰를 수정합니다.")
    @PatchMapping
    public ResponseEntity<APIResponse<Void>> updateReview(@RequestBody @Valid @Parameter(description = "수정할 리뷰 내용", required = true) ReviewUpdateRequestDto dto) {
        try {
            reviewService.updateReview(dto);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(summary = "리뷰 삭제", description = "구매자가 자신의 리뷰(리뷰, 이미지, 매핑 테이블 모두) 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<APIResponse<Void>> deleteReview(
            @RequestBody @Valid @Parameter(description = "삭제할 리뷰 id", required = true) ReviewDeleteRequestDto dto) {
        try {
            reviewService.deleteReview(dto);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS));
        } catch (NoSuchElementException e) {
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
