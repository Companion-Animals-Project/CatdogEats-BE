package com.team5.catdogeats.products.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.products.domain.dto.ProductCreateRequestDto;
import com.team5.catdogeats.products.domain.dto.ProductUpdateRequestDto;
import com.team5.catdogeats.products.domain.enums.ProductCategory;
import com.team5.catdogeats.products.domain.enums.StockStatus;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products")
public class Products extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "product_number", nullable = false, unique = true)
    private Long productNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", referencedColumnName = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_products_seller_id"))
    private Sellers seller;

    @Column(length = 50, nullable = false)
    private String title;

    @Column(nullable = false)
    private String subTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String productInfo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String contents;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PetCategory petCategory;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProductCategory productCategory;

    @Column(name = "discounted")
    @Builder.Default
    private Boolean discounted = false;

    @Min(0)
    @Max(100)
    @Column(name = "discount_rate",
            nullable = false,
            columnDefinition = "SMALLINT")
    private short discountRate;

    @Column(nullable = false)
    private Long price;

    @Column(name = "discounted_price")
    private Long discountedPrice;

    @Column(name = "lead_time", nullable = false)
    private Short leadTime;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "safety_stock", nullable = false)
    private int safetyStock;

    @Version // 동시성 제어
    private Long version;

    @Transient
    public StockStatus getStockStatus() {
        if (stock <= 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        if (stock <= safetyStock) {
            return StockStatus.LOW_STOCK;
        }
        return StockStatus.IN_STOCK;
    }

    public void decreaseStock(int qty) {
        if (this.stock < qty) throw new IllegalArgumentException("재고 부족");
        this.stock -= qty;
    }

    public static Products fromDto(ProductCreateRequestDto dto, Sellers seller, Long productNumber) {
        return Products.builder()
                .productNumber(productNumber)
                .seller(seller)
                .title(dto.title())
                .subTitle(dto.subTitle())
                .productInfo(dto.productInfo())
                .contents(dto.contents())
                .petCategory(dto.petCategory())
                .productCategory(dto.productCategory())
                .discounted(dto.isDiscounted())
                .discountRate(dto.discountRate())
                .price(dto.price())
                .discountedPrice(dto.isDiscounted() != true ? (long) (dto.price() * (dto.discountRate() * 0.01)) : dto.price())
                .leadTime(dto.leadTime())
                .stock(dto.stock())
                .safetyStock(dto.stock()/2)
                .build();
    }

    public void updateFromDto(ProductUpdateRequestDto dto) {
        if (dto.title() != null) this.title = dto.title();
        if (dto.subTitle() != null) this.subTitle = dto.subTitle();
        if (dto.productInfo() != null) this.productInfo = dto.productInfo();
        if (dto.contents() != null) this.contents = dto.contents();
        if (dto.petCategory() != null) this.petCategory = dto.petCategory();
        if (dto.productCategory() != null) this.productCategory = dto.productCategory();
        if (dto.isDiscounted() != null) this.discounted = dto.isDiscounted();
        if (dto.discountRate() != null) this.discountRate = dto.discountRate();
        if (dto.price() != null) this.price = dto.price();
        if (dto.leadTime() != null) this.leadTime = dto.leadTime();
        if (dto.stock() != null) this.stock = dto.stock();
    }
}
