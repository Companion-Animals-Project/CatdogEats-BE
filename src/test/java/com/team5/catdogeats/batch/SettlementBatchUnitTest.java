package com.team5.catdogeats.batch;

import com.team5.catdogeats.batch.config.SettlementBatchProperties;
import com.team5.catdogeats.batch.dto.SettlementBatchItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;


class SettlementBatchUnitTest {

    @Test
    @DisplayName("SettlementBatchProperties 설정 테스트")
    void testSettlementBatchProperties() {
        // Given
        SettlementBatchProperties properties = new SettlementBatchProperties();

        // When - 기본값 설정
        properties.setEnabled(true);
        properties.setChunkSize(1000);
        properties.setSkipLimit(100);
        properties.setRetryLimit(3);
        properties.setCommissionRate(new BigDecimal("0.1"));
        properties.setConfirmationPeriodDays(7);

        // Then
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getChunkSize()).isEqualTo(1000);
        assertThat(properties.getSkipLimit()).isEqualTo(100);
        assertThat(properties.getRetryLimit()).isEqualTo(3);
        assertThat(properties.getCommissionRate()).isEqualTo(new BigDecimal("0.1"));
        assertThat(properties.getConfirmationPeriodDays()).isEqualTo(7);

        System.out.println("✅ SettlementBatchProperties 모든 설정이 올바릅니다!");
    }

    @Test
    @DisplayName("SettlementBatchProperties 유효성 검증 테스트")
    void testSettlementBatchPropertiesValidation() {
        // Given
        SettlementBatchProperties properties = new SettlementBatchProperties();

        // When - 유효한 값들 설정
        properties.setChunkSize(500);
        properties.setSkipLimit(50);
        properties.setRetryLimit(2);
        properties.setCommissionRate(new BigDecimal("0.15"));
        properties.setConfirmationPeriodDays(10);

        // Then - 유효성 검증이 통과해야 함
        try {
            properties.validate();
            System.out.println("✅ 유효성 검증 통과!");
        } catch (Exception e) {
            throw new AssertionError("유효한 설정에서 예외가 발생했습니다: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("SettlementBatchItem 생성 테스트")
    void testSettlementBatchItemCreation() {
        // Given
        String sellerId = "seller123";
        String orderItemId = "item456";
        String orderNumber = "ORDER_789";
        String productTitle = "테스트 상품";
        Long itemPrice = 10000L;
        BigDecimal commissionRate = new BigDecimal("0.1");

        // When - 정산 생성용 DTO 생성
        SettlementBatchItem item = SettlementBatchItem.forCreate(
                sellerId, orderItemId, orderNumber, productTitle, itemPrice, commissionRate
        );

        // Then
        assertThat(item.getSellerId()).isEqualTo(sellerId);
        assertThat(item.getOrderItemId()).isEqualTo(orderItemId);
        assertThat(item.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(item.getProductTitle()).isEqualTo(productTitle);
        assertThat(item.getItemPrice()).isEqualTo(itemPrice);
        assertThat(item.getCommissionRate()).isEqualTo(commissionRate);
        assertThat(item.getCommissionAmount()).isEqualTo(1000L); // 10000 * 0.1
        assertThat(item.getSettlementAmount()).isEqualTo(9000L); // 10000 - 1000

        System.out.println("✅ SettlementBatchItem 생성 성공!");
        System.out.println("정산 금액: " + item.getSettlementAmount());
        System.out.println("수수료: " + item.getCommissionAmount());
    }

    @Test
    @DisplayName("SettlementBatchItem 완료용 생성 테스트")
    void testSettlementBatchItemForComplete() {
        // Given
        String settlementId = "settlement123";
        String sellerId = "seller456";
        String orderNumber = "ORDER_789";
        String productTitle = "완료 테스트 상품";
        Long settlementAmount = 9000L;

        // When - 정산 완료용 DTO 생성
        SettlementBatchItem item = SettlementBatchItem.forComplete(
                settlementId, sellerId, orderNumber, productTitle, settlementAmount
        );

        // Then
        assertThat(item.getSettlementId()).isEqualTo(settlementId);
        assertThat(item.getSellerId()).isEqualTo(sellerId);
        assertThat(item.getOrderNumber()).isEqualTo(orderNumber);
        assertThat(item.getProductTitle()).isEqualTo(productTitle);
        assertThat(item.getSettlementAmount()).isEqualTo(settlementAmount);

        System.out.println("✅ 정산 완료용 SettlementBatchItem 생성 성공!");
    }

    @Test
    @DisplayName("SettlementBatchItem 유효성 검증 테스트")
    void testSettlementBatchItemValidation() {
        // Given - 유효한 생성용 아이템
        SettlementBatchItem createItem = SettlementBatchItem.forCreate(
                "seller123", "item456", "ORDER_789", "상품", 10000L, new BigDecimal("0.1")
        );

        // Given - 유효한 완료용 아이템
        SettlementBatchItem completeItem = SettlementBatchItem.forComplete(
                "settlement123", "seller456", "ORDER_789", "상품", 9000L
        );

        // When & Then
        assertThat(createItem.isValidForCreate()).isTrue();
        assertThat(completeItem.isValidForComplete()).isTrue();

        System.out.println("✅ SettlementBatchItem 유효성 검증 통과!");
    }

    @Test
    @DisplayName("수수료 계산 정확성 테스트")
    void testCommissionCalculation() {
        // Given - 다양한 가격과 수수료율
        Long[] prices = {10000L, 25000L, 50000L, 100000L};
        BigDecimal[] rates = {new BigDecimal("0.05"), new BigDecimal("0.1"), new BigDecimal("0.15")};

        for (Long price : prices) {
            for (BigDecimal rate : rates) {
                // When
                SettlementBatchItem item = SettlementBatchItem.forCreate(
                        "seller", "item", "order", "product", price, rate
                );

                // Then - 수수료 계산 검증
                Long expectedCommission = price.longValue() * rate.multiply(new BigDecimal("100")).intValue() / 100;
                Long expectedSettlement = price - expectedCommission;

                assertThat(item.getCommissionAmount()).isEqualTo(expectedCommission);
                assertThat(item.getSettlementAmount()).isEqualTo(expectedSettlement);
            }
        }

        System.out.println("✅ 모든 수수료 계산이 정확합니다!");
    }

    @Test
    @DisplayName("배치 설정 toString 테스트")
    void testToStringMethods() {
        // Given
        SettlementBatchItem item = SettlementBatchItem.forCreate(
                "seller123", "item456", "ORDER_789", "테스트 상품", 10000L, new BigDecimal("0.1")
        );

        // When
        String toString = item.toString();

        // Then
        assertThat(toString).isNotNull();
        assertThat(toString).contains("seller123");
        assertThat(toString).contains("ORDER_789");
        assertThat(toString).contains("테스트 상품");

        System.out.println("✅ toString 출력:");
        System.out.println(toString);
    }

    @Test
    @DisplayName("종합 시나리오 테스트")
    void testSettlementBatchScenario() {
        // Given - 배치 설정
        SettlementBatchProperties properties = new SettlementBatchProperties();
        properties.setEnabled(true);
        properties.setChunkSize(100);
        properties.setCommissionRate(new BigDecimal("0.1"));

        // When - 배치 아이템 생성 시뮬레이션
        SettlementBatchItem item1 = SettlementBatchItem.forCreate(
                "seller1", "item1", "ORDER_001", "상품A", 15000L, properties.getCommissionRate()
        );

        SettlementBatchItem item2 = SettlementBatchItem.forCreate(
                "seller2", "item2", "ORDER_002", "상품B", 25000L, properties.getCommissionRate()
        );

        // Then - 시나리오 검증
        assertThat(item1.isValidForCreate()).isTrue();
        assertThat(item2.isValidForCreate()).isTrue();

        // 수수료 계산 검증
        assertThat(item1.getCommissionAmount()).isEqualTo(1500L); // 15000 * 0.1
        assertThat(item1.getSettlementAmount()).isEqualTo(13500L); // 15000 - 1500

        assertThat(item2.getCommissionAmount()).isEqualTo(2500L); // 25000 * 0.1
        assertThat(item2.getSettlementAmount()).isEqualTo(22500L); // 25000 - 2500

        System.out.println("✅ 종합 시나리오 테스트 완료!");
        System.out.println("총 정산 금액: " + (item1.getSettlementAmount() + item2.getSettlementAmount()));
        System.out.println("총 수수료: " + (item1.getCommissionAmount() + item2.getCommissionAmount()));
    }
}