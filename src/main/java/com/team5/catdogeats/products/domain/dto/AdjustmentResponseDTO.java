package com.team5.catdogeats.products.domain.dto;

import com.team5.catdogeats.products.domain.enums.StockStatus;

import java.time.ZonedDateTime;

public record AdjustmentResponseDTO(String id,
                                    String title,
                                    String productNumber,
                                    int stock,
                                    StockStatus status,
                                    Long price,
                                    Long discountedPrice,
                                    StockStatus stockStatus,
                                    ZonedDateTime updatedAt) {

}
