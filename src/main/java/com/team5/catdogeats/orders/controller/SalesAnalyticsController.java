package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.orders.domain.dto.PeriodSalesAnalyticsResponseDTO;
import com.team5.catdogeats.orders.domain.dto.ProductSalesAnalyticsRequestDTO;
import com.team5.catdogeats.orders.domain.dto.ProductSalesAnalyticsResponseDTO;
import com.team5.catdogeats.orders.domain.enums.SalesAnalyticsType;
import com.team5.catdogeats.orders.service.SalesAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Year;
import java.util.NoSuchElementException;

/**
 * 매출 분석 관리 컨트롤러
 * 정산 완료된 데이터 기반으로 매출 분석 기능 제공
 * SettlementController 패턴을 참고하여 구현
 */
@Slf4j
@RestController
@RequestMapping("/v1/sellers/analytics/sales")
@RequiredArgsConstructor
@Tag(name = "매출 분석 관리", description = "판매자 매출 분석 조회 API")
public class SalesAnalyticsController {

    private final SalesAnalyticsService salesAnalyticsService;

    /**
     * 기간별 매출 분석 조회 (년도별 월별 집계)
     * API: GET /v1/sellers/analytics/sales/period?year={year}
     *
     * 응답 데이터:
     * - 1~12월 각 월별 매출총액
     * - 선택년도 매출총액
     * - 각 월별 주문건수
     * - 각 월별 판매수량
     */
    @GetMapping("/period")
    @Operation(
            summary = "기간별 매출 분석 조회",
            description = "판매자의 특정 년도 월별 매출 분석 데이터를 조회합니다. (정산 완료된 데이터만)"
    )
    public ResponseEntity<ApiResponse<PeriodSalesAnalyticsResponseDTO>> getPeriodSalesAnalytics(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "조회할 년도 (2020~현재년도)", example = "2024")
            @RequestParam Integer year) {

        try {
            log.info("기간별 매출 분석 조회 요청 - provider: {}, providerId: {}, year: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), year);

            // 년도 유효성 검증
            validateYear(year);

            PeriodSalesAnalyticsResponseDTO response = salesAnalyticsService.getPeriodSalesAnalytics(userPrincipal, year);

            log.info("기간별 매출 분석 조회 성공 - year: {}, 년도총매출: {}, 월별데이터수: {}",
                    year, response.yearTotalAmount(), response.monthlyData().size());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (IllegalArgumentException e) {
            log.warn("기간별 매출 분석 조회 실패 - 잘못된 년도: {}, 사유: {}", year, e.getMessage());
            return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (NoSuchElementException e) {
            log.warn("기간별 매출 분석 조회 실패 - 판매자를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (Exception e) {
            log.error("기간별 매출 분석 조회 중 예상치 못한 오류", e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "기간별 매출 분석 조회 중 서버 오류가 발생했습니다"));
        }
    }

    /**
     * 상품별 매출 분석 조회 (연도별/월별 + 페이징)

     * 응답 데이터:
     * - 상품별 판매액, 상품이름, 판매개수
     * - 전체 대비 퍼센트
     * - 해당 기간 매출 총액
     * - 페이징 (크기 8개)
     */
    @GetMapping("/products")
    @Operation(
            summary = "상품별 매출 분석 조회",
            description = "판매자의 상품별 매출 분석 데이터를 조회합니다. 연도별/월별 선택 가능하며 페이징 처리됩니다. (정산 완료된 데이터만)"
    )
    public ResponseEntity<ApiResponse<ProductSalesAnalyticsResponseDTO>> getProductSalesAnalytics(
            @AuthenticationPrincipal UserPrincipal userPrincipal,

            @Parameter(description = "조회 타입 (Swagger에서 드롭다운으로 표시)")
            @RequestParam SalesAnalyticsType type,

            @Parameter(description = "조회할 년도 (2020~현재년도)", example = "2024")
            @RequestParam Integer year,

            @Parameter(description = "조회할 월 (monthly 타입인 경우 필수, 1~12)", example = "3")
            @RequestParam(required = false) Integer month,

            @PageableDefault(size = 8) Pageable pageable) {

        try {
            log.info("상품별 매출 분석 조회 요청 - provider: {}, providerId: {}, type: {}, year: {}, month: {}, page: {}, size: {}",
                    userPrincipal.provider(), userPrincipal.providerId(),
                    type, year, month, pageable.getPageNumber(), pageable.getPageSize());

            // 요청 파라미터 객체 생성 및 검증
            ProductSalesAnalyticsRequestDTO request = new ProductSalesAnalyticsRequestDTO(type, year, month);
            request.validate(); // 유효성 검증

            ProductSalesAnalyticsResponseDTO response = salesAnalyticsService.getProductSalesAnalytics(
                    userPrincipal, request, pageable);

            log.info("상품별 매출 분석 조회 성공 - type: {}, year: {}, month: {}, 총매출: {}, 상품수: {}",
                    type, year, month, response.totalAmount(), response.products().totalElements());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (IllegalArgumentException e) {
            log.warn("상품별 매출 분석 조회 실패 - 잘못된 요청: type={}, year={}, month={}, 사유: {}",
                    type, year, month, e.getMessage());
            return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (NoSuchElementException e) {
            log.warn("상품별 매출 분석 조회 실패 - 판매자를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (Exception e) {
            log.error("상품별 매출 분석 조회 중 예상치 못한 오류", e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "상품별 매출 분석 조회 중 서버 오류가 발생했습니다"));
        }
    }

    /**
     * 년도 유효성 검증 (2020년 ~ 현재년도)
     * 프론트에서도 검증하지만 백엔드에서도 보안상 검증
     */
    private void validateYear(Integer year) {
        if (year == null) {
            throw new IllegalArgumentException("년도는 필수입니다");
        }

        int currentYear = Year.now().getValue();
        if (year < 2020 || year > currentYear) {
            throw new IllegalArgumentException(
                    String.format("년도는 2020년부터 %d년까지만 허용됩니다", currentYear));
        }
    }
}
