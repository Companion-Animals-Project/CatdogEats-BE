package com.team5.catdogeats.forecast.mapper;

import com.team5.catdogeats.forecast.domain.DemandForecasts;
import com.team5.catdogeats.forecast.domain.dto.DemandForecastResultDTO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DemandForecastMapper {

    /**
     * 수요예측 결과 저장
     */
    @Insert("""
    INSERT INTO demand_forecasts (
        id, seller_id, product_id, forecast_date, 
        prediction_period_days, predicted_quantity, algorithm_type,
        confidence_score, historical_data_days,
        created_at, updated_at
    ) VALUES (
        #{id}, #{seller.userId}, #{product.id}, #{forecastDate},
        #{predictionPeriodDays}, #{predictedQuantity}, #{algorithmType},
        #{confidenceScore}, #{historicalDataDays},
        #{createdAt}, #{updatedAt}
    )
    """)
    void insertForecast(DemandForecasts forecast);


    /**
     * 특정 판매자의 최신 수요예측 결과 조회 (재고 부족량 계산용)
     */
    @Select("""
        SELECT 
            df.id,
            df.seller_id,
            df.product_id,
            p.title as product_name,
            p.stock as current_stock,
            df.predicted_quantity,
            df.algorithm_type,
            df.confidence_score,
            df.forecast_date,
            CASE 
                WHEN p.stock < df.predicted_quantity 
                THEN df.predicted_quantity - p.stock 
                ELSE 0 
            END as shortage_quantity
        FROM demand_forecasts df
        INNER JOIN products p ON df.product_id = p.id
        WHERE df.seller_id = #{sellerId}
        AND df.forecast_date = (
            SELECT MAX(df2.forecast_date)
            FROM demand_forecasts df2
            WHERE df2.seller_id = #{sellerId}
            AND df2.product_id = df.product_id
        )
        ORDER BY shortage_quantity DESC, df.predicted_quantity DESC
        """)
    @ConstructorArgs({
            @Arg(column = "id", javaType = String.class),
            @Arg(column = "seller_id", javaType = String.class),
            @Arg(column = "product_id", javaType = String.class),
            @Arg(column = "product_name", javaType = String.class),
            @Arg(column = "current_stock", javaType = Integer.class),
            @Arg(column = "predicted_quantity", javaType = Integer.class),
            @Arg(column = "shortage_quantity", javaType = Integer.class),
            @Arg(column = "algorithm_type", javaType = String.class),
            @Arg(column = "confidence_score", javaType = Double.class),
            @Arg(column = "forecast_date", javaType = java.time.LocalDate.class)
    })
    List<DemandForecastResultDTO> findLatestForecastsWithStockBySellerId(@Param("sellerId") String sellerId);



    /**
     * 오래된 예측 데이터 삭제 (성능 관리용)
     */
    @Delete("""
        DELETE FROM demand_forecasts 
        WHERE forecast_date < #{cutoffDate}
        """)
    int deleteOldForecasts(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * 예측 데이터 일괄 삭제 (배치 재실행용)
     */
    @Delete("""
        DELETE FROM demand_forecasts 
        WHERE seller_id = #{sellerId} AND forecast_date = #{forecastDate}
        """)
    int deleteForecastsBySellerAndDate(
            @Param("sellerId") String sellerId,
            @Param("forecastDate") LocalDate forecastDate);
}