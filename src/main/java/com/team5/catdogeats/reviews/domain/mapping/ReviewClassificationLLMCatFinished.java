package com.team5.catdogeats.reviews.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.reviews.domain.Reviews;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_classification_result_cat_finished")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewClassificationLLMCatFinished extends BaseEntity {
    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Reviews review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Products product;

    @Column(name = "result", nullable = false, columnDefinition = "TEXT")
    private String result; // "true"/"false"

    @Column(name = "sentiment", nullable = false, columnDefinition = "TEXT")
    private String sentiment; // "positive"/"negative"/""

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "review_content", nullable = false, columnDefinition = "TEXT")
    private String reviewContent;
}
