package com.team5.catdogeats.forecast.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "demand_forecasts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class DemandForecasts extends BaseEntity {

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

    @Column(name = "forecast_date", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "prediction_period_days", nullable = false)
    @Builder.Default
    private Integer predictionPeriodDays = 7;

    @Column(name = "predicted_quantity", nullable = false)
    private Integer predictedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm_type", nullable = false)
    private AlgorithmType algorithmType;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "historical_data_days")
    private Integer historicalDataDays;

    public enum AlgorithmType {
        MOVING_AVERAGE_7,
        EXPONENTIAL_SMOOTHING,
        SEASONAL_ADJUSTMENT
    }
}

