package com.team5.catdogeats.batch.mapper;

import com.team5.catdogeats.reviews.domain.mapping.ReviewsSummaryLLM;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ReviewSummaryLLMBatchMapper {
    @Select("""
        SELECT * FROM reviews_summary_llm
         WHERE product_id = #{productId}
         ORDER BY created_at DESC
         LIMIT 1
    """)
    @Results(id="SummaryResultMap", value={
            @Result(property = "reviewCount", column = "review_count"),
            @Result(property = "createdAt", column = "created_at", typeHandler = com.team5.catdogeats.batch.handler.ZonedDateTimeTypeHandler.class),
            @Result(property = "updatedAt", column = "updated_at", typeHandler = com.team5.catdogeats.batch.handler.ZonedDateTimeTypeHandler.class)
    })
    ReviewsSummaryLLM findTopByProductIdOrderByCreatedAtDesc(@Param("productId") String productId);

    @Insert("""
        INSERT INTO reviews_summary_llm(id, product_id, positive_review, negative_review, review_count,positive_main_points, negative_main_points, positive_keyword, negative_keyword, created_at, updated_at)
        VALUES (#{id}, #{product.id}, #{positiveReview}, #{negativeReview}, #{reviewCount}, #{positiveMainPoints}, #{negativeMainPoints}, #{positiveKeyword}, #{negativeKeyword}, now(), now())
    """)
    void insertSummary(ReviewsSummaryLLM summary);

    @Delete("DELETE FROM reviews_summary_llm WHERE product_id = #{productId}")
    void deleteByProductId(@Param("productId") String productId);
}
