package com.team5.catdogeats.storage.service.impl;

import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.storage.domain.Images;
import com.team5.catdogeats.storage.domain.mapping.ReviewsImages;
import com.team5.catdogeats.storage.exception.ImageUploadException;
import com.team5.catdogeats.storage.repository.ImageRepository;
import com.team5.catdogeats.storage.repository.ReviewImageRepository;
import com.team5.catdogeats.storage.service.ReviewImageDBHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewImageDBHelperImpl implements ReviewImageDBHelper {
    private final ImageRepository imageRepository;
    private final ReviewImageRepository reviewImageRepository;

    @Override
    @Retryable(
            retryFor = { Exception.class },
            backoff = @Backoff(delay = 1000)
    )
    public Images storeImageToDbWithRetry(String imageUrl) {
        log.info("[storeImageToDbWithRetry] Called. imageUrl={}", imageUrl);

        // 테스트: "fail" 포함 시 강제 에러
        if (imageUrl.contains("fail")) {
            log.warn("[storeImageToDbWithRetry] 강제로 예외 발생시킴! (imageUrl에 'fail' 포함)");
            throw new RuntimeException("강제 테스트 예외 발생!");
        }

        Images image = Images.builder().imageUrl(imageUrl).build();
        return imageRepository.save(image);
    }

    @Recover
    public Images recoverStoreImageToDb(Exception e, String imageUrl) {
        log.error("[storeImageToDbWithRetry][Recover] 모든 재시도 실패! imageUrl={}, error={}", imageUrl, e.getMessage());
        throw new ImageUploadException("DB(Images) 저장 최종 실패: " + e.getMessage());
    }

    @Override
    @Retryable(
            retryFor = { Exception.class },
            backoff = @Backoff(delay = 1000)
    )
    public void storeReviewImageMappingWithRetry(Reviews review, Images image) {
        log.info("[storeReviewImageMappingWithRetry] Called. reviewId={}, imageId={}",
                review.getId(), image.getId());

        ReviewsImages reviewImages = ReviewsImages.builder()
                .reviews(review)
                .images(image)
                .build();
        reviewImageRepository.save(reviewImages);
    }

    @Recover
    public void recoverStoreReviewImageMapping(Exception e, Reviews review, Images image) {
        log.error("[storeReviewImageMappingWithRetry][Recover] 모든 재시도 실패! reviewId={}, imageId={}, error={}",
                review.getId(), image.getId(), e.getMessage());
        throw new ImageUploadException("DB(reviews_images) 매핑 최종 실패: " + e.getMessage());
    }
}
