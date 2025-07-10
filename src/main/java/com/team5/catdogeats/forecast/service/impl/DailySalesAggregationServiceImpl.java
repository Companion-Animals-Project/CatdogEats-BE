package com.team5.catdogeats.forecast.service.impl;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.forecast.domain.DailySalesAggregation;
import com.team5.catdogeats.forecast.domain.dto.DailySalesDataDTO;
import com.team5.catdogeats.forecast.mapper.DailySalesAggregationMapper;
import com.team5.catdogeats.forecast.service.DailySalesAggregationService;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
@MybatisTransactional
public class DailySalesAggregationServiceImpl implements DailySalesAggregationService {

    private final DailySalesAggregationMapper dailySalesMapper;


    /**
     * 특정 날짜의 일별 판매 데이터 집계
     * @param targetDate 집계 대상 날짜
     * @return 집계 처리된 레코드 수
     */
    @Override
    public int aggregateDailySales(LocalDate targetDate) {
        log.info("일별 판매 집계 시작 - targetDate: {}", targetDate);

        try {
            // 1. 원본 주문 데이터에서 일별 집계 추출 (MyBatis)
            List<DailySalesDataDTO> aggregatedData = dailySalesMapper.aggregateDailySalesByDate(targetDate);

            if (aggregatedData.isEmpty()) {
                log.info("집계할 판매 데이터가 없습니다 - targetDate: {}", targetDate);
                return 0;
            }

            log.info("집계 대상 데이터 수: {} - targetDate: {}", aggregatedData.size(), targetDate);

            // 2. 집계 데이터를 daily_sales_aggregation 테이블에 저장
            int processedCount = 0;
            for (DailySalesDataDTO data : aggregatedData) {
                try {
                    // 기존 데이터 존재 여부 확인하여 중복 방지
                    boolean exists = dailySalesMapper.existsBySellerAndProductAndDate(
                            data.sellerId(), data.productId(), data.salesDate());

                    if (!exists) {
                        saveDailySalesAggregation(data);
                        processedCount++;
                    } else {
                        // 이미 존재하는 경우 업데이트 (UPSERT)
                        updateDailySalesAggregation(data);
                        processedCount++;
                    }

                } catch (Exception e) {
                    log.error("개별 집계 데이터 처리 실패 - sellerId: {}, productId: {}, date: {}",
                            data.sellerId(), data.productId(), data.salesDate(), e);
                }
            }

            log.info("일별 판매 집계 완료 - targetDate: {}, 처리 건수: {}/{}",
                    targetDate, processedCount, aggregatedData.size());
            return processedCount;

        } catch (Exception e) {
            log.error("일별 판매 집계 실패 - targetDate: {}", targetDate, e);
            throw new RuntimeException("일별 판매 집계 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 새로운 집계 데이터 저장 (타임스탬프 명시적 설정)
     */
    private void saveDailySalesAggregation(DailySalesDataDTO data) {
        try {

            Sellers seller = dailySalesMapper.findSellerById(data.sellerId());
            Products product = dailySalesMapper.findProductById(data.productId());

            if (seller == null) {
                log.warn("판매자를 찾을 수 없습니다 - sellerId: {}", data.sellerId());
                return;
            }

            if (product == null) {
                log.warn("상품을 찾을 수 없습니다 - productId: {}", data.productId());
                return;
            }

            ZonedDateTime now = ZonedDateTime.now();

            DailySalesAggregation aggregation = DailySalesAggregation.builder()
                    .id(UUID.randomUUID().toString())
                    .seller(seller)
                    .product(product)
                    .salesDate(data.salesDate())
                    .dailyQuantity(data.dailyQuantity())
                    .dailyRevenue(data.dailyRevenue())
                    .orderCount(data.orderCount())
                    .build();

            // BaseEntity 필드 직접 설정 (reflection으로)
            setTimestamps(aggregation, now, now);

            // MyBatis로 저장
            dailySalesMapper.upsertDailySales(aggregation);

        } catch (Exception e) {
            log.error("집계 데이터 저장 실패 - data: {}", data, e);
            throw e;
        }
    }

    /**
     * 기존 집계 데이터 업데이트 (타임스탬프 명시적 설정)
     */
    private void updateDailySalesAggregation(DailySalesDataDTO data) {
        try {
            // MyBatis로 Seller, Product 조회
            Sellers seller = dailySalesMapper.findSellerById(data.sellerId());
            Products product = dailySalesMapper.findProductById(data.productId());

            if (seller == null || product == null) {
                log.warn("업데이트 실패 - 데이터 없음: sellerId={}, productId={}",
                        data.sellerId(), data.productId());
                return;
            }

            ZonedDateTime now = ZonedDateTime.now();

            DailySalesAggregation aggregation = DailySalesAggregation.builder()
                    .id(UUID.randomUUID().toString()) // UPSERT에서는 ID가 중요하지 않음
                    .seller(seller)
                    .product(product)
                    .salesDate(data.salesDate())
                    .dailyQuantity(data.dailyQuantity())
                    .dailyRevenue(data.dailyRevenue())
                    .orderCount(data.orderCount())
                    .build();

            // BaseEntity 필드 직접 설정 (기존 생성일 유지, 수정일만 업데이트)
            setTimestamps(aggregation, null, now); // createdAt은 null로 두어 기존값 유지

            // MyBatis로 UPSERT (업데이트)
            dailySalesMapper.upsertDailySales(aggregation);

        } catch (Exception e) {
            log.error("집계 데이터 업데이트 실패 - data: {}", data, e);
            throw e;
        }
    }

    /**
     * BaseEntity의 타임스탬프 필드를 설정하는 헬퍼 메서드
     */
    private void setTimestamps(DailySalesAggregation aggregation, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        try {
            java.lang.reflect.Field createdAtField = BaseEntity.class.getDeclaredField("createdAt");
            java.lang.reflect.Field updatedAtField = BaseEntity.class.getDeclaredField("updatedAt");

            createdAtField.setAccessible(true);
            updatedAtField.setAccessible(true);

            if (createdAt != null) {
                createdAtField.set(aggregation, createdAt);
            }

            if (updatedAt != null) {
                updatedAtField.set(aggregation, updatedAt);
            }

        } catch (Exception e) {
            log.error("타임스탬프 설정 실패", e);
            throw new RuntimeException("타임스탬프 설정 중 오류 발생", e);
        }
    }

}