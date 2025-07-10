package com.team5.catdogeats.batch.config;

import com.team5.catdogeats.batch.dto.ProductReviewBatchDto;
import com.team5.catdogeats.batch.mapper.ProductBatchMapper;
import com.team5.catdogeats.batch.mapper.ReviewBatchMapper;
import com.team5.catdogeats.batch.mapper.ReviewSummaryLLMBatchMapper;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.mapping.ReviewsSummaryLLM;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@StepScope
@Slf4j
public class ProductReviewBatchDtoItemReader implements ItemReader<ProductReviewBatchDto> {
    private final ProductBatchMapper productMapper;
    private final ReviewBatchMapper reviewMapper;
    private final ReviewSummaryLLMBatchMapper summaryMapper;
    private final String petCategory;
    private final String productCategory;

    private int index = 0;

    public ProductReviewBatchDtoItemReader(ProductBatchMapper productMapper,
                                           ReviewBatchMapper reviewMapper,
                                           ReviewSummaryLLMBatchMapper summaryMapper,
                                           @Value("#{jobParameters['petCategory']}") String petCategory,
                                           @Value("#{jobParameters['productCategory']}") String productCategory) {
        this.productMapper = productMapper;
        this.reviewMapper = reviewMapper;
        this.summaryMapper = summaryMapper;
        this.petCategory = petCategory;
        this.productCategory = productCategory;
    }

    @Override
    public ProductReviewBatchDto read() {
        try {
            List<Products> products = productMapper.selectProductsByCategory(petCategory, productCategory);
            if (index < products.size()) {
                Products product = products.get(index++);
                List<Reviews> reviews = reviewMapper.findTop30ByProductIdOrderByCreatedAtDesc(product.getId());
                ReviewsSummaryLLM summary = summaryMapper.findTopByProductIdOrderByCreatedAtDesc(product.getId());
                log.info("[Reader] read() - 상품: {}, index={}/{}", product.getTitle(), index, products.size());
                return new ProductReviewBatchDto(product, reviews, summary);
            }
        } catch (Exception e) {
            log.error("[배치리더] ProductReviewBatchDto read 중 에러: {}", e.getMessage(), e);
        }
        return null;
    }
}
