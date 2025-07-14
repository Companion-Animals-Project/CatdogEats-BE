package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.batch.dto.ReviewSummaryResult;
import com.team5.catdogeats.batch.mapper.ReviewClassificationLLMCatHandmadeBatchMapper;
import com.team5.catdogeats.batch.mapper.ReviewSummaryLLMBatchMapper;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.reviews.domain.dto.ReviewClassificationResultDto;
import com.team5.catdogeats.reviews.domain.mapping.ReviewClassificationLLMCatHandmade;
import com.team5.catdogeats.reviews.domain.mapping.ReviewsSummaryLLM;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

import java.util.UUID;

@Slf4j
@StepScope
public class ReviewSummaryItemWriterCatHandmade implements ItemWriter<ReviewSummaryResult> {
    private final ReviewSummaryLLMBatchMapper summaryMapper;
    private final ReviewClassificationLLMCatHandmadeBatchMapper classificationMapper;

    public ReviewSummaryItemWriterCatHandmade(ReviewSummaryLLMBatchMapper summaryMapper,
                                              ReviewClassificationLLMCatHandmadeBatchMapper classificationMapper) {
        this.summaryMapper = summaryMapper;
        this.classificationMapper = classificationMapper;
    }

    @Override
    public void write(Chunk<? extends ReviewSummaryResult> items) {
        try {
            for (ReviewSummaryResult result : items) {
                // 변화 없으면 분류, 요약 결과 그대로 사용
                if(result == null) {
                    log.info("[배치요약] 상품 : 리뷰 변화 없음, skip");
                    continue;
                }

                Products product = result.product();
                if (product == null) {
                    log.error("[배치요약] product를 찾을 수 없음. 이 결과는 skip");
                    continue;
                }

                // 기존 리뷰분류 결과 삭제
                classificationMapper.deleteAllByProductId(product.getId());


                // 분류결과 새로 저장
                for (ReviewClassificationResultDto dto : result.classificationResults()) {
                    ReviewClassificationLLMCatHandmade entity = ReviewClassificationLLMCatHandmade.builder()
                            .id(UUID.randomUUID().toString())
                            .product(product)
                            .review(dto.reviewObj())
                            .result(dto.result())
                            .sentiment(dto.sentiment())
                            .reason(dto.reason())
                            .reviewContent(dto.review())
                            .build();
                    log.info("[배치요약] 상품 {}: 분류 새로 저장", product.getTitle());
                    classificationMapper.insert(entity);
                }

                // 기존 리뷰요약 결과 삭제
                summaryMapper.deleteByProductId(product.getId());
                // 요약결과 새로 저장
                ReviewsSummaryLLM summary = ReviewsSummaryLLM.builder()
                        .id(UUID.randomUUID().toString())
                        .product(product)
                        .positiveReview(result.positiveJson())
                        .negativeReview(result.negativeJson())
                        .reviewCount(result.reviewCount())
                        .positiveMainPoints(result.positiveMainPoints())
                        .negativeMainPoints(result.negativeMainPoints())
                        .positiveKeyword(result.positiveKeywords())
                        .negativeKeyword(result.negativeKeywords())
                        .build();
                log.info("[배치요약] 상품 {}: 요약 새로 저장", product.getTitle());
                summaryMapper.insertSummary(summary);
            }
        } catch (Exception e) {
            log.error("[배치요약] 상품 요약 저장 중 에러: {}", e.getMessage(), e);
        }
    }
}
