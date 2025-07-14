package com.team5.catdogeats.users.domain.dto;

import com.team5.catdogeats.users.domain.mapping.Buyers;

import java.time.OffsetDateTime;

public record BuyerDTO(String userId,
                       boolean nameMaskingStatus,
                       boolean isDeleted,
                       OffsetDateTime deletedAt) {

    public static Buyers toEntity(BuyerDTO buyerDTO){
        return Buyers.builder()
                .userId(buyerDTO.userId())
                .nameMaskingStatus(buyerDTO.nameMaskingStatus())
                .isDeleted(buyerDTO.isDeleted())
                .deledAt(buyerDTO.deletedAt())
                .build();
    }
}
