package com.team5.catdogeats.carts.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductComparisonRequest {
    @NotNull
    private String product1Id;

    @NotNull
    private String product2Id;

    // 펫 정보 (프론트엔드에서 전달)
    private String petCategory; // "강아지" | "고양이"
    private String breed;
    private Integer age;
    private String gender;

    // 추가 정보 (프론트에서 사용 중)
    private Boolean hasAllergies;
    private String healthCondition;
    private String specialRequests;
}