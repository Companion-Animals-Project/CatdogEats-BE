package com.team5.catdogeats.batch.mapper;

import com.team5.catdogeats.reviews.domain.Reviews;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReviewBatchMapper {
    @Select("""
        SELECT * FROM reviews
         WHERE product_id = #{productId}
         ORDER BY created_at DESC
         LIMIT 30
    """)
    @Results(id="ReviewsResultMap", value = {
            @Result(property = "createdAt", column = "created_at", typeHandler = com.team5.catdogeats.batch.handler.ZonedDateTimeTypeHandler.class),
            @Result(property = "updatedAt", column = "updated_at", typeHandler = com.team5.catdogeats.batch.handler.ZonedDateTimeTypeHandler.class)
    })
    List<Reviews> findTop30ByProductIdOrderByCreatedAtDesc(@Param("productId") String productId);
}
