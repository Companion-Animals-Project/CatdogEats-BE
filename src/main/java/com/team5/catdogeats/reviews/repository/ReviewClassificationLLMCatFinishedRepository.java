package com.team5.catdogeats.reviews.repository;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.mapping.ReviewClassificationLLMCatFinished;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewClassificationLLMCatFinishedRepository extends JpaRepository<ReviewClassificationLLMCatFinished, String> {
    @Modifying
    @Query("DELETE FROM ReviewClassificationLLMCatFinished r WHERE r.product = :product")
    void deleteAllByProduct(@Param("product") Products product);

    void deleteAllByReview(Reviews review);
}
