package com.team5.catdogeats.storage.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.storage.domain.dto.ReviewImageUploadResponseDto;
import com.team5.catdogeats.storage.exception.ImageUploadException;
import com.team5.catdogeats.storage.service.ReviewImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/buyers/reviews/images")
@Tag(name = "ReviewImage", description = "리뷰 이미지 관련 API")
public class ReviewImageController {

    private final ReviewImageService reviewImageService;

    // 리뷰 이미지 등록 (S3 업로드 + Images DB 저장 + reviews_images 매핑)
    @Operation(
            summary = "리뷰 이미지 업로드",
            description = "여러 장의 이미지를 한 번에 업로드합니다."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<List<ReviewImageUploadResponseDto>>> uploadReviewImage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "이미지를 업로드할 리뷰 id", required = true)
            @RequestParam String reviewId,
            @Parameter(description = "업로드할 이미지 파일 리스트", required = true)
            @RequestPart("images") List<MultipartFile> images) {
        try {
            List<ReviewImageUploadResponseDto> response = reviewImageService.uploadReviewImage(userPrincipal, reviewId, images);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));
        } catch (ImageUploadException e) {
            // DB저장, 매핑 저장 등 업로드 과정 오류
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(APIResponse.error(ResponseCode.INVALID_TYPE_VALUE, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }

    @Operation(
            summary = "리뷰 이미지 수정",
            description = "여러 장의 이미지를 한 번에 수정합니다."
    )
    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse<List<ReviewImageUploadResponseDto>>> updateReviewImage(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "이미지를 수정할 리뷰 id", required = true)
            @RequestParam String reviewId,
            @Parameter(description = "수정할 이미지 ids", required = true)
            @RequestParam List<String> oldImageIds,
            @Parameter(description = "새로 업로드할 이미지 파일 리스트", required = true)
            @RequestPart List<MultipartFile> images
    ) {
        try {
            List<ReviewImageUploadResponseDto> response = reviewImageService.updateReviewImage(userPrincipal, reviewId, oldImageIds, images);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));
        } catch (ImageUploadException e) {
            // DB저장, 매핑 저장 등 업로드 과정 오류
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(APIResponse.error(ResponseCode.INVALID_TYPE_VALUE, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
        }
    }
}
