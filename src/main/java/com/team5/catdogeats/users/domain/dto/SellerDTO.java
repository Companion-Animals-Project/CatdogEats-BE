package com.team5.catdogeats.users.domain.dto;

import com.team5.catdogeats.users.domain.mapping.Sellers;

import java.time.LocalTime;
import java.time.OffsetDateTime;

public record SellerDTO(String userId,
                        String vendorName,
                        String vendorProfileImage,
                        String businessNumber,
                        String settlementBank,
                        String settlementAccount,
                        String tags,
                        LocalTime operatingStartTime,
                        LocalTime operatingEndTime,
                        String closedDays,
                        boolean isDeleted,
                        OffsetDateTime deletedAt) {
    public static Sellers toEntity(SellerDTO sellerDTO){
        return Sellers.builder()
                .userId(sellerDTO.userId())
                .vendorName(sellerDTO.vendorName())
                .vendorProfileImage(sellerDTO.vendorName())
                .businessNumber(sellerDTO.businessNumber())
                .settlementBank(sellerDTO.settlementBank())
                .settlementAccount(sellerDTO.settlementBank())
                .tags(sellerDTO.tags())
                .operatingStartTime(sellerDTO.operatingStartTime())
                .operatingEndTime(sellerDTO.operatingEndTime())
                .closedDays(sellerDTO.closedDays())
                .isDeleted(sellerDTO.isDeleted())
                .deledAt(sellerDTO.deletedAt())
                .build();
    }
}
