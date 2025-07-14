package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.MybatisTransactional;
import com.team5.catdogeats.orders.domain.dto.*;
import com.team5.catdogeats.orders.mapper.SalesAnalyticsMapper;
import com.team5.catdogeats.orders.service.SalesAnalyticsService;
import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.repository.SellersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 매출 분석 서비스 구현체
 * SettlementServiceImpl 패턴을 참고하여 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@MybatisTransactional(readOnly = true)
public class SalesAnalyticsServiceImpl implements SalesAnalyticsService {

    private final SalesAnalyticsMapper salesAnalyticsMapper;
    private final SellersRepository sellerRepository;

    @Override
    public PeriodSalesAnalyticsResponseDTO getPeriodSalesAnalytics(UserPrincipal userPrincipal, Integer year) {
        log.info("기간별 매출 분석 조회 시작 - provider: {}, providerId: {}, year: {}",
                userPrincipal.provider(), userPrincipal.providerId(), year);

        try {
            // 1. 판매자 권한 검증 및 ID 조회
            String sellerId = validateAndGetSellerId(userPrincipal);

            // 2. 년도별 총 매출 요약 조회
            MonthlySalesRawDataDTO yearTotalSales = salesAnalyticsMapper.findYearTotalSalesBySellerAndYear(sellerId, year);

            // 3. 월별 매출 데이터 조회 (1~12월 모든 데이터)
            List<MonthlySalesRawDataDTO> monthlySalesRawDatumDTOS = salesAnalyticsMapper.findMonthlySalesBySellerAndYear(sellerId, year);

            // 4. 월별 데이터 변환 (MonthlySalesRawDataDTO -> MonthlySalesDataDTO)
            List<MonthlySalesDataDTO> monthlyData = monthlySalesRawDatumDTOS.stream()
                    .map(raw -> new MonthlySalesDataDTO(
                            raw.month(),
                            raw.totalAmount(),
                            raw.orderCount(),
                            raw.totalQuantity()
                    ))
                    .toList();

            // 5. 응답 객체 생성
            PeriodSalesAnalyticsResponseDTO response = new PeriodSalesAnalyticsResponseDTO(
                    year,
                    yearTotalSales.totalAmount(),      // 년도 총 매출액
                    yearTotalSales.orderCount(),       // 년도 총 주문건수
                    yearTotalSales.totalQuantity(),    // 년도 총 판매수량
                    monthlyData
            );

            log.info("기간별 매출 분석 조회 완료 - sellerId: {}, year: {}, 년도총매출: {}, 월별데이터수: {}",
                    sellerId, year, yearTotalSales.totalAmount(), monthlyData.size());

            return response;

        } catch (Exception e) {
            log.error("기간별 매출 분석 조회 실패 - year: {}", year, e);
            throw e;
        }
    }

    @Override
    public ProductSalesAnalyticsResponseDTO getProductSalesAnalytics(
            UserPrincipal userPrincipal,
            ProductSalesAnalyticsRequestDTO request,
            Pageable pageable) {

        log.info("상품별 매출 분석 조회 시작 - provider: {}, providerId: {}, type: {}, year: {}, month: {}, page: {}, size: {}",
                userPrincipal.provider(), userPrincipal.providerId(),
                request.type(), request.year(), request.month(),
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            // 1. 요청 파라미터 검증
            request.validate();

            // 2. 판매자 권한 검증 및 ID 조회
            String sellerId = validateAndGetSellerId(userPrincipal);

            // 3. 페이징 정보 계산
            long offset = pageable.getOffset();
            int limit = pageable.getPageSize();

            // 4. 타입별 처리 분기
            if (request.isYearly()) {
                return processYearlyProductSales(sellerId, request.year(), offset, limit, pageable);
            } else {
                return processMonthlyProductSales(sellerId, request.year(), request.month(), offset, limit, pageable);
            }

        } catch (Exception e) {
            log.error("상품별 매출 분석 조회 실패 - request: {}", request, e);
            throw e;
        }
    }

    /**
     * 연도별 상품 매출 분석 처리
     */
    private ProductSalesAnalyticsResponseDTO processYearlyProductSales(
            String sellerId, Integer year, long offset, int limit, Pageable pageable) {

        // 1. 상품별 매출 데이터 조회
        List<ProductSalesRawDataDTO> productSalesRawDatumDTOS = salesAnalyticsMapper
                .findYearlyProductSalesBySellerAndYear(sellerId, year, offset, limit);

        // 2. 전체 상품 개수 조회 (페이징용)
        Long totalElements = salesAnalyticsMapper.countYearlyProductsBySellerAndYear(sellerId, year);

        // 3. 총 매출액 조회 (퍼센트 계산용)
        TotalSalesAmountDTO totalSalesAmountDTO = salesAnalyticsMapper.findYearlyTotalAmountBySellerAndYear(sellerId, year);

        // 4. 퍼센트 계산 및 응답 데이터 변환
        List<ProductSalesDataDTO> productSalesDatumDTOS = calculatePercentageAndConvert(
                productSalesRawDatumDTOS, totalSalesAmountDTO.totalAmount());

        // 5. 페이징 응답 생성
        ProductSalesPageResponseDTO pageResponse = createPageResponse(
                productSalesDatumDTOS, totalElements, pageable);

        // 6. 최종 응답 생성
        ProductSalesAnalyticsResponseDTO response = new ProductSalesAnalyticsResponseDTO(
                "yearly", year, null, totalSalesAmountDTO.totalAmount(), pageResponse);

        log.info("연도별 상품 매출 분석 완료 - sellerId: {}, year: {}, 총매출: {}, 상품수: {}",
                sellerId, year, totalSalesAmountDTO.totalAmount(), totalElements);

        return response;
    }

    /**
     * 월별 상품 매출 분석 처리
     */
    private ProductSalesAnalyticsResponseDTO processMonthlyProductSales(
            String sellerId, Integer year, Integer month, long offset, int limit, Pageable pageable) {

        // 1. 상품별 매출 데이터 조회
        List<ProductSalesRawDataDTO> productSalesRawDatumDTOS = salesAnalyticsMapper
                .findMonthlyProductSalesBySellerAndYearMonth(sellerId, year, month, offset, limit);

        // 2. 전체 상품 개수 조회 (페이징용)
        Long totalElements = salesAnalyticsMapper.countMonthlyProductsBySellerAndYearMonth(sellerId, year, month);

        // 3. 총 매출액 조회 (퍼센트 계산용)
        TotalSalesAmountDTO totalSalesAmountDTO = salesAnalyticsMapper.findMonthlyTotalAmountBySellerAndYearMonth(sellerId, year, month);

        // 4. 퍼센트 계산 및 응답 데이터 변환
        List<ProductSalesDataDTO> productSalesDatumDTOS = calculatePercentageAndConvert(
                productSalesRawDatumDTOS, totalSalesAmountDTO.totalAmount());

        // 5. 페이징 응답 생성
        ProductSalesPageResponseDTO pageResponse = createPageResponse(
                productSalesDatumDTOS, totalElements, pageable);

        // 6. 최종 응답 생성
        ProductSalesAnalyticsResponseDTO response = new ProductSalesAnalyticsResponseDTO(
                "monthly", year, month, totalSalesAmountDTO.totalAmount(), pageResponse);

        log.info("월별 상품 매출 분석 완료 - sellerId: {}, year: {}, month: {}, 총매출: {}, 상품수: {}",
                sellerId, year, month, totalSalesAmountDTO.totalAmount(), totalElements);

        return response;
    }

    /**
     * 퍼센트 계산 및 데이터 변환
     * 서비스에서 퍼센트를 계산하여 성능 최적화
     */
    private List<ProductSalesDataDTO> calculatePercentageAndConvert(
            List<ProductSalesRawDataDTO> rawData, Long totalAmount) {

        if (totalAmount == 0L || rawData.isEmpty()) {
            return rawData.stream()
                    .map(raw -> new ProductSalesDataDTO(
                            raw.productId(), raw.productName(),
                            raw.totalAmount(), raw.quantity(), 0.0))
                    .toList();
        }

        return rawData.stream()
                .map(raw -> {
                    // 퍼센트 계산 (소수점 둘째자리까지)
                    double percentage = Math.round((raw.totalAmount() * 100.0 / totalAmount) * 100.0) / 100.0;

                    return new ProductSalesDataDTO(
                            raw.productId(),
                            raw.productName(),
                            raw.totalAmount(),
                            raw.quantity(),
                            percentage
                    );
                })
                .toList();
    }

    /**
     * 페이징 응답 객체 생성
     */
    private ProductSalesPageResponseDTO createPageResponse(
            List<ProductSalesDataDTO> content, Long totalElements, Pageable pageable) {

        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / pageable.getPageSize());
        boolean isFirst = pageable.getPageNumber() == 0;
        boolean isLast = pageable.getPageNumber() >= totalPages - 1;

        return new ProductSalesPageResponseDTO(
                content,
                totalElements,
                pageable.getPageSize(),
                pageable.getPageNumber(),
                totalPages,
                isFirst,
                isLast
        );
    }


    /**
     * UserPrincipal로 판매자 검증 및 ID 조회
     * SettlementServiceImpl과 동일한 패턴 사용
     */
    private String validateAndGetSellerId(UserPrincipal userPrincipal) {
        SellerDTO sellerDTO = sellerRepository.findSellerDtoByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> {
                    log.warn("판매자를 찾을 수 없음 - provider: {}, providerId: {}",
                            userPrincipal.provider(), userPrincipal.providerId());
                    return new NoSuchElementException("판매자 정보를 찾을 수 없습니다");
                });

        return sellerDTO.userId();
    }
}
