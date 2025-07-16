package com.team5.catdogeats.forecast.domain.dto;

import lombok.Builder;

/**
 * 배치 처리용 판매자 수요예측 DTO
 */
@Builder
public record SellerForecastDTO(
        String sellerId,
        String vendorName,
        boolean isActive,
        int processedProducts,
        boolean success,
        String errorMessage,
        long processingTimeMs
) {

    /**
     * 성공한 결과 생성
     */
    public static SellerForecastDTO success(String sellerId, String vendorName,
                                            int processedProducts, long processingTimeMs) {
        return SellerForecastDTO.builder()
                .sellerId(sellerId)
                .vendorName(vendorName)
                .isActive(true)
                .processedProducts(processedProducts)
                .success(true)
                .processingTimeMs(processingTimeMs)
                .build();
    }

    /**
     * 실패한 결과 생성
     */
    public static SellerForecastDTO failure(String sellerId, String vendorName,
                                            String errorMessage, long processingTimeMs) {
        return SellerForecastDTO.builder()
                .sellerId(sellerId)
                .vendorName(vendorName)
                .isActive(true)
                .processedProducts(0)
                .success(false)
                .errorMessage(errorMessage)
                .processingTimeMs(processingTimeMs)
                .build();
    }
}