package com.team5.catdogeats.orders.domain.dto;

import com.team5.catdogeats.orders.domain.enums.SettlementStatus;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * 정산 아이템 상세 정보 DTO
 * 정산 리스트의 개별 아이템 정보를 담는 Record
 */
public record SettlementItemDTO(
        String orderNumber,      // 주문번호
        String productName,      // 상품명
        Long orderAmount,        // 주문금액
        Long commission,         // 수수료 (10%)
        Long settlementAmount,   // 정산금액 (주문금액 - 수수료)
        LocalDateTime orderDate, // 주문일
        LocalDateTime deliveryDate, // 배송완료일
        LocalDateTime settlementCreatedAt,//정산 생성일
        SettlementStatus status  // 정산상태
) {
}
