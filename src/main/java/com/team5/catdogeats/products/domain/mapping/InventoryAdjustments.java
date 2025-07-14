package com.team5.catdogeats.products.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.enums.AdjustmentType;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Table(name = "inventory_adjustments")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryAdjustments extends BaseEntity {

    @Id
    @Column(name = "id", length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "product_id",
            referencedColumnName = "id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_inventory_adjustments_product")
    )
    private Products products;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id",
            referencedColumnName = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_inventory_adjustments_seller"))
    private Sellers sellers;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type")
    private AdjustmentType adjustmentType;

    @Min(1)
    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "note")
    private String note;
}
