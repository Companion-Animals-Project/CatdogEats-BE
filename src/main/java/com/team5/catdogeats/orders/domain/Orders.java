package com.team5.catdogeats.orders.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Orders extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_buyer_id"))
    private Buyers buyers;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    @Column(name = "sub_total_price", nullable = false)
    private Long subtotalPrice;

    @Column(name = "total_delivery_fee", nullable = false)
    private Long totalDeliveryFee;

    @Column(name = "total_discount_amount", nullable = false)
    private Long totalDiscountAmount;

    @Column(name = "discounted_total_price", nullable = false)
    private Long discountedTotalPrice;

    // ===== 주문 내역 숨김 기능 필드 추가 =====
    /**
     * 주문 내역 숨김 여부
     * true: 사용자에게 보이지 않음, false: 정상 표시
     */
    @Column(name = "is_hidden", nullable = false)
    @Builder.Default
    private Boolean isHidden = Boolean.FALSE;

    /**
     * 주문 내역 숨김 처리 시각
     * 숨김 처리된 정확한 시점을 기록 (추후 복원 등에 활용 가능)
     */
    @Column(name = "hidden_at")
    private ZonedDateTime hiddenAt;

    // 주문 상품 목록
    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY)
    private List<OrderItems> orderItems;

    // 배송 정보 (일대일 관계)
    @OneToOne(mappedBy = "orders", cascade = CascadeType.ALL)
    private Shipments shipment;

    // ===== 주문 내역 숨김 기능 편의 메서드 =====
    /**
     * 주문 내역을 숨김 처리하는 메서드
     * 숨김 여부를 true로 설정하고 숨김 처리 시각을 현재 시간으로 기록
     */
    public void hideOrder() {
        this.isHidden = Boolean.TRUE;
        this.hiddenAt = ZonedDateTime.now();
    }

    /**
     * 주문 내역이 숨겨진 상태인지 확인하는 메서드
     * @return 숨김 상태 여부
     */
    public boolean isOrderHidden() {
        return Boolean.TRUE.equals(this.isHidden);
    }

    public void updateOderStatus(OrderStatus orderStatus) {
        this.orderStatus=orderStatus;
    }
}