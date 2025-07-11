package com.team5.catdogeats.carts.domain.mapping;

import com.team5.catdogeats.carts.domain.Carts;
import com.team5.catdogeats.products.domain.Products;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class CartItems {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cart_items_cart"))
    private Carts carts;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cart_items_product"))
    private Products product;

    @Column(name = "quantity", nullable = false)
    @Min(value = 1, message = "수량은 1개 이상이어야 합니다")
    @Max(value = 10, message = "수량은 10개 이하여야 합니다")
    private int quantity;

    @Column(name = "added_at", insertable = false, updatable = false)
    private ZonedDateTime addedAt;
}
