package com.team5.catdogeats.reviews.domain.dto;

import com.team5.catdogeats.pets.domain.dto.PetInfoResponseDto;
import com.team5.catdogeats.reviews.util.MaskingUtil;
import com.team5.catdogeats.storage.domain.dto.ReviewImageResponseDto;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public class ProductReviewResponseDtoBuilder {
    private final String id;
    private final String writerName;
    private final Boolean nameMaskingStatus;
    private final Double star;
    private final String contents;
    private final String updatedAt;
    private final LinkedHashSet<PetInfoResponseDto> pets = new LinkedHashSet<>();
    private final LinkedHashSet<ReviewImageResponseDto> images = new LinkedHashSet<>();

    public ProductReviewResponseDtoBuilder(String id, String writerName, Boolean nameMaskingStatus, Double star, String contents, String updatedAt) {
        this.id = id;
        this.writerName = writerName;
        this.nameMaskingStatus = nameMaskingStatus;
        this.star = star;
        this.contents = contents;
        this.updatedAt = updatedAt;
    }

    public void addPet(PetInfoResponseDto pet) { if (pet != null) pets.add(pet); }
    public void addImage(ReviewImageResponseDto img) { if (img != null) images.add(img); }

    public ProductReviewResponseDto build() {
        // 마스킹 유틸 적용
        String displayName = (Boolean.TRUE.equals(nameMaskingStatus)) ? MaskingUtil.maskName(writerName) : writerName;

        return new ProductReviewResponseDto(
                id,
                displayName,
                new ArrayList<>(pets),   // 리스트로 변환
                star,
                contents,
                updatedAt,
                new ArrayList<>(images)  // 리스트로 변환
        );
    }
}
