package com.team5.catdogeats.storage.service;

import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.storage.domain.Images;

public interface ReviewImageDBHelper {
    Images storeImageToDbWithRetry(String imageUrl);
    void storeReviewImageMappingWithRetry(Reviews review, Images image);
}
