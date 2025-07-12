package com.team5.catdogeats.carts.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductComparisonResponse {

    // 비교된 제품 정보
    private ProductInfo product1;
    private ProductInfo product2;

    // AI 분석 결과
    private String analysis;           // 전체 분석 내용
    private String recommendation;     // 최종 추천 결과
    private String nutritionAnalysis;  // 영양성분 분석
    private String priceAnalysis;      // 가격 분석
    private String petSuitability;     // 반려동물 적합성 분석

    // 반려동물 정보 (입력받은 정보 반환)
    private PetInfo petInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {
        private String id;
        private String name;
        private String option;
        private Long price;
        private String image;
        private String category;  // 강아지용/고양이용
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PetInfo {
        private String petCategory;     // "강아지" | "고양이"
        private String breed;
        private Integer age;
        private String gender;
        private Boolean hasAllergies;
        private String healthCondition;
        private String specialRequests;
    }
}