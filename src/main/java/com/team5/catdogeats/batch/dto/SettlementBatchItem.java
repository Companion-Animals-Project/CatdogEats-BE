package com.team5.catdogeats.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 정산 배치 처리용 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementBatchItem {

    /**
     * 정산 ID (COMPLETE 작업 시 사용)
     */
    private String settlementId;

    /**
     * 판매자 ID
     */
    private String sellerId;

    /**
     * 주문 아이템 ID
     */
    private String orderItemId;

    /**
     * 주문 번호 (로깅용)
     */
    private String orderNumber;

    /**
     * 상품명 (로깅용)
     */
    private String productTitle;

    /**
     * 아이템 가격
     */
    private Long itemPrice;

    /**
     * 수수료율
     */
    private BigDecimal commissionRate;

    /**
     * 수수료 금액
     */
    private Long commissionAmount;

    /**
     * 정산 금액
     */
    private Long settlementAmount;

    /**
     * 배송 완료일
     */
    private OffsetDateTime deliveredAt;

    /**
     * 처리 타입별 생성자들
     */

    /**
     * 정산 생성용 생성자 - 바로 IN_PROGRESS 상태로 생성
     */
    public static SettlementBatchItem forCreate(String sellerId, String orderItemId,
                                                String orderNumber, String productTitle,
                                                Long itemPrice, BigDecimal commissionRate) {
        Long commissionAmount = calculateCommission(itemPrice, commissionRate);
        Long settlementAmount = itemPrice - commissionAmount;

        return SettlementBatchItem.builder()
                .sellerId(sellerId)
                .orderItemId(orderItemId)
                .orderNumber(orderNumber)
                .productTitle(productTitle)
                .itemPrice(itemPrice)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .settlementAmount(settlementAmount)
                .build();
    }

    /**
     * 정산 완료용 생성자 - IN_PROGRESS → COMPLETED
     */
    public static SettlementBatchItem forComplete(String settlementId, String sellerId,
                                                  String orderNumber, String productTitle,
                                                  Long settlementAmount) {
        return SettlementBatchItem.builder()
                .settlementId(settlementId)
                .sellerId(sellerId)
                .orderNumber(orderNumber)
                .productTitle(productTitle)
                .settlementAmount(settlementAmount)
                .build();
    }

    /**
     * 수수료 계산 메서드
     */
    private static Long calculateCommission(Long itemPrice, BigDecimal commissionRate) {
        if (itemPrice == null || commissionRate == null) {
            return 0L;
        }
        return BigDecimal.valueOf(itemPrice)
                .multiply(commissionRate)
                .setScale(0, BigDecimal.ROUND_HALF_UP)
                .longValue();
    }

    /**
     * 로깅용 문자열 표현
     */
    @Override
    public String toString() {
        return String.format("SettlementBatchItem{id='%s', sellerId='%s', orderNumber='%s', " +
                        "productTitle='%s', itemPrice=%d, settlementAmount=%d}",
                settlementId, sellerId, orderNumber, productTitle, itemPrice, settlementAmount);
    }


    /**
     * 정산 생성용 데이터 유효성 검증
     */
    public boolean isValidForCreate() {
        return sellerId != null && !sellerId.trim().isEmpty()
                && orderItemId != null && !orderItemId.trim().isEmpty()
                && itemPrice != null && itemPrice > 0
                && commissionRate != null && commissionRate.compareTo(BigDecimal.ZERO) >= 0;
    }

    /**
     * 정산 완료용 데이터 유효성 검증
     */
    public boolean isValidForComplete() {
        return settlementId != null && !settlementId.trim().isEmpty()
                && settlementAmount != null && settlementAmount >= 0;
    }
}