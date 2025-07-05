    package com.team5.catdogeats.coupons.domain.mapping;

    import com.team5.catdogeats.baseEntity.BaseEntity;
    import com.team5.catdogeats.coupons.domain.Coupons;
    import com.team5.catdogeats.users.domain.mapping.Sellers;
    import jakarta.persistence.*;
    import lombok.*;

    @Entity
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    @Builder
    @Table(name = "seller_coupons",
            uniqueConstraints = @UniqueConstraint(columnNames = {"coupon_id", "seller_id"}))
    public class SellerCoupons extends BaseEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private String id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "coupon_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_seller_coupons_coupon_id"))
        private Coupons coupons;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "seller_id", nullable = false,
                    foreignKey = @ForeignKey(name = "fk_seller_coupons_seller_id"))
        private Sellers sellers;
    }
