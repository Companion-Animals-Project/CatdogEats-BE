package com.team5.catdogeats.batch.mapper;

import com.team5.catdogeats.reviews.domain.mapping.ReviewClassificationLLMDogFinished;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReviewClassificationLLMDogFinishedBatchMapper {
    @Insert("""
        INSERT INTO review_classification_result_dog_finished(id, review_id, product_id, result, sentiment, reason, review_content, created_at, updated_at)
        VALUES (#{id}, #{review.id}, #{product.id}, #{result}, #{sentiment}, #{reason}, #{reviewContent}, now(), now())
    """)
    void insert(ReviewClassificationLLMDogFinished entity);

    @Delete("DELETE FROM review_classification_result_dog_finished WHERE product_id = #{productId}")
    void deleteAllByProductId(@Param("productId") String productId);
}