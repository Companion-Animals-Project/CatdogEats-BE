package com.team5.catdogeats.orders.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.users.domain.Users;
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
    private Long orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_user_id"))
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

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
    @OneToOne(mappedBy = "orders", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Shipments shipment;

    // ===== 주문 내역 숨김 기능 편의 메서드 =====
    /**
     * 주문 내역을 숨김 처리하는 메서드
     * 숨김 여부를 true로 설정하고 숨김 처리 시각을 현재 시간으로 기록합니다.
     */
    public void hideOrder() {
        this.isHidden = Boolean.TRUE;
        this.hiddenAt = ZonedDateTime.now();
    }

    /**
     * 주문 내역 숨김을 해제하는 메서드 (복원 기능)
     * 숨김 여부를 false로 설정하고 숨김 처리 시각을 null로 초기화합니다.
     */
    public void unhideOrder() {
        this.isHidden = Boolean.FALSE;
        this.hiddenAt = null;
    }

    /**
     * 주문 내역이 숨겨진 상태인지 확인하는 메서드
     * @return 숨김 상태 여부
     */
    public boolean isOrderHidden() {
        return Boolean.TRUE.equals(this.isHidden);
    }

    /**
     * 주문이 숨김 처리 가능한 상태인지 확인하는 메서드
     * 이미 숨겨진 주문은 중복 처리하지 않도록 체크
     * @return 숨김 처리 가능 여부
     */
    public boolean canBeHidden() {
        return !Boolean.TRUE.equals(this.isHidden);
    }
}