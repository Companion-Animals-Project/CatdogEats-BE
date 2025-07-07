package com.team5.catdogeats.reviews.repository;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.mapping.ReviewClassificationLLM;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewClassificationLLMRepository extends JpaRepository<ReviewClassificationLLM, String> {
    void deleteAllByProduct(Products product);

    void deleteAllByReview(Reviews review);
}
