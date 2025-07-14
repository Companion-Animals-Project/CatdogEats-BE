package com.team5.catdogeats.carts.dto;

import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;

public class TestBuyerDTO {
    public static BuyerDTO from(Buyers buyers) {
        return new BuyerDTO(
                buyers.getUserId(),
                buyers.isNameMaskingStatus(),
                buyers.isDeleted(),
                buyers.getDeledAt()
        );
    }
}
