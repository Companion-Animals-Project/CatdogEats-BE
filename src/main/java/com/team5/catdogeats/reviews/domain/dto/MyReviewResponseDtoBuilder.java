package com.team5.catdogeats.reviews.domain.dto;

import com.team5.catdogeats.storage.domain.dto.ReviewImageResponseDto;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class MyReviewResponseDtoBuilder {
    private final String id;
    private final String productName;
    private final Double star;
    private final String contents;
    private final String updatedAt;
    private final LinkedHashSet<ReviewImageResponseDto> images = new LinkedHashSet<>();
    public MyReviewResponseDtoBuilder(String id, String productName, Double star, String contents, String updatedAt) {
        this.id = id; this.productName = productName; this.star = star; this.contents = contents; this.updatedAt = updatedAt;
    }
    public void addImage(ReviewImageResponseDto img) { if (img != null) images.add(img); }
    public MyReviewResponseDto build() {
        return new MyReviewResponseDto(id, productName, star, contents, updatedAt, new ArrayList<>(images));
    }
}