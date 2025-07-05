package com.team5.catdogeats.coupons.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.coupons.domain.enums.DiscountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Coupons extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 50, name = "code", unique = true)
    private String code;

    @Column(nullable = false, length = 50, name = "coupon_name")
    private String couponName;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 7)
    private DiscountType discountType;

    @PositiveOrZero
    @Column(name = "discount_value", nullable = false)
    private Integer discountValue;

    @Column(name = "start_date", nullable = false, columnDefinition = "DATE")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false, columnDefinition = "DATE")
    private LocalDate endDate;

    @PositiveOrZero
    @Column(name = "usage_limit")
    @Builder.Default
    private Integer usageLimit = 0;

}
