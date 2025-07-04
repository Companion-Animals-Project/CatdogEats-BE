package com.team5.catdogeats.orders.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.orders.domain.enums.SettlementStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "settlements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Settlements extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_settlements_seller"))
    private Sellers seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_settlements_order_item"))
    private OrderItems orderItems;

    @Column(name = "item_price", nullable = false)
    private Long itemPrice;

    @Column(name = "commission_rate", columnDefinition = "DECIMAL(5,2)", nullable = false)
    private BigDecimal commissionRate;

    @Column(name = "commission_amount", nullable = false)
    private Long commissionAmount;

    @Column(name = "settlement_amount", nullable = false)
    private Long settlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", length = 15, nullable = false)
    @Builder.Default
    private SettlementStatus settlementStatus = SettlementStatus.PENDING;

    @Column(name = "settled_at")
    private ZonedDateTime settledAt;
// ===== 정산 상태 관리 메서드 =====

    /**
     * 정산 상태를 변경하는 메서드
     * @param newStatus 새로운 정산 상태
     */
    public void updateStatus(SettlementStatus newStatus) {
        this.settlementStatus = newStatus;
    }

    /**
     * 정산을 완료 상태로 변경하고 완료 시각을 설정
     */
    public void completeSettlement() {
        this.settlementStatus = SettlementStatus.COMPLETED;
        this.settledAt = ZonedDateTime.now();
    }

    /**
     * 정산이 완료된 상태인지 확인
     * @return 정산 완료 여부
     */
    public boolean isCompleted() {
        return this.settlementStatus == SettlementStatus.COMPLETED;
    }

    /**
     * 정산이 처리중인 상태인지 확인
     * @return 처리중 여부
     */
    public boolean isInProgress() {
        return this.settlementStatus == SettlementStatus.IN_PROGRESS;
    }

    /**
     * 정산이 대기중인 상태인지 확인
     * @return 대기중 여부
     */
    public boolean isPending() {
        return this.settlementStatus == SettlementStatus.PENDING;
    }

    /**
     * 정산 상태를 다음 단계로 진행
     * PENDING -> IN_PROGRESS -> COMPLETED 순서로 진행
     * @return 상태 변경 성공 여부
     */
    public boolean progressToNextStatus() {
        switch (this.settlementStatus) {
            case PENDING:
                this.settlementStatus = SettlementStatus.IN_PROGRESS;
                return true;
            case IN_PROGRESS:
                completeSettlement();
                return true;
            case COMPLETED:
                // 이미 완료된 상태는 변경하지 않음
                return false;
            default:
                return false;
        }
    }

    /**
     * 정산 금액 정보 업데이트
     * 가격이나 수수료율이 변경되었을 때 사용
     * @param itemPrice 상품 가격
     * @param commissionRate 수수료율
     */
    public void updateSettlementAmounts(Long itemPrice, BigDecimal commissionRate) {
        this.itemPrice = itemPrice;
        this.commissionRate = commissionRate;

        // 수수료 계산
        BigDecimal itemPriceBigDecimal = BigDecimal.valueOf(itemPrice);
        BigDecimal commission = itemPriceBigDecimal
                .multiply(commissionRate)
                .divide(BigDecimal.valueOf(100), 0, BigDecimal.ROUND_HALF_UP);

        this.commissionAmount = commission.longValue();
        this.settlementAmount = itemPrice - this.commissionAmount;
    }
}