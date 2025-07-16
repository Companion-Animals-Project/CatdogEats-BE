package com.team5.catdogeats.forecast.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_sales_aggregation",
        indexes = {
                @Index(name = "idx_daily_sales_seller_product_date",
                        columnList = "seller_id, product_id, sales_date"),
                @Index(name = "idx_daily_sales_date_range",
                        columnList = "sales_date"),
                @Index(name = "idx_daily_sales_seller_date",
                        columnList = "seller_id, sales_date")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_daily_sales_seller_product_date",
                        columnNames = {"seller_id", "product_id", "sales_date"}
                )
        })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DailySalesAggregation extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Sellers seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products product;

    @Column(name = "sales_date", nullable = false)
    private LocalDate salesDate;

    @Column(name = "daily_quantity", nullable = false)
    @Builder.Default
    private Integer dailyQuantity = 0;

    @Column(name = "daily_revenue", nullable = false)
    @Builder.Default
    private Long dailyRevenue = 0L;

    @Column(name = "order_count")
    @Builder.Default
    private Integer orderCount = 0;
}