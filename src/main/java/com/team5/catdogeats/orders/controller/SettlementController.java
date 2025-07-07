package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.orders.domain.dto.MonthlySettlementReceiptDto;
import com.team5.catdogeats.orders.domain.dto.MonthlySettlementStatusDto;
import com.team5.catdogeats.orders.domain.dto.SettlementListResponseDto;
import com.team5.catdogeats.orders.domain.dto.SettlementPeriodRequestDto;
import com.team5.catdogeats.orders.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.NoSuchElementException;

/**
 * 정산현황 관리 컨트롤러 (수정된 버전)
 * Settlement 테이블 기반으로만 조회 (배송완료 후 7일 지난 데이터만)
 * pendingAmount 제거, inProgressAmount 추가
 */
@Slf4j
@RestController
@RequestMapping("/v1/sellers/settlements")
@RequiredArgsConstructor
@Tag(name = "정산현황 관리", description = "판매자 정산현황 조회 API")
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * 전체 정산 리스트 조회 (페이징)
     * 1-1. 주문번호, 상품명, 주문금액, 수수료, 정산금액, 주문일, 상태의 정산 리스트 출력 (페이징)
     * 1-2. 총 정산내역 건수, 총 정산금액, 완료금액, 처리중금액
     */
    @GetMapping
    @Operation(
            summary = "전체 정산 리스트 조회",
            description = "판매자의 전체 정산 내역을 페이징으로 조회합니다. (Settlement 테이블 데이터만, 배송완료 후 7일 지난 데이터)"
    )
    public ResponseEntity<ApiResponse<SettlementListResponseDto>> getSettlementList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20) Pageable pageable) {

        try {
            log.info("전체 정산 리스트 조회 요청 - provider: {}, providerId: {}, page: {}, size: {}",
                    userPrincipal.provider(), userPrincipal.providerId(),
                    pageable.getPageNumber(), pageable.getPageSize());

            SettlementListResponseDto response = settlementService.getSettlementList(userPrincipal, pageable);

            log.info("전체 정산 리스트 조회 성공 - 총건수: {}, 현재페이지건수: {}",
                    response.settlements().getTotalElements(), response.settlements().getNumberOfElements());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("정산 리스트 조회 실패 - 판매자를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("정산 리스트 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("정산 리스트 조회 중 예상치 못한 오류", e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "정산 리스트 조회 중 서버 오류가 발생했습니다"));
        }
    }

    /**
     * 기간별 정산 리스트 조회 (페이징)
     * 2. 사용자가 선택한 기간동안의 정산 리스트 출력 (페이징)
     * 2-2. 선택한 기간동안의 정산내역 건수, 총 정산금액, 완료금액, 처리중금액
     */
    @PostMapping("/period")
    @Operation(
            summary = "기간별 정산 리스트 조회",
            description = "판매자의 지정 기간 정산 내역을 페이징으로 조회합니다. (Settlement 테이블 데이터만)"
    )
    public ResponseEntity<ApiResponse<SettlementListResponseDto>> getSettlementListByPeriod(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody SettlementPeriodRequestDto periodRequest,
            @PageableDefault(size = 20) Pageable pageable) {

        try {
            log.info("기간별 정산 리스트 조회 요청 - provider: {}, providerId: {}, 기간: {} ~ {}, page: {}, size: {}",
                    userPrincipal.provider(), userPrincipal.providerId(),
                    periodRequest.startDate(), periodRequest.endDate(),
                    pageable.getPageNumber(), pageable.getPageSize());

            SettlementListResponseDto response = settlementService.getSettlementListByPeriod(
                    userPrincipal, periodRequest, pageable);

            log.info("기간별 정산 리스트 조회 성공 - 총건수: {}, 현재페이지건수: {}",
                    response.settlements().getTotalElements(), response.settlements().getNumberOfElements());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("기간별 정산 리스트 조회 실패 - 판매자를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("기간별 정산 리스트 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("기간별 정산 리스트 조회 중 예상치 못한 오류", e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "기간별 정산 리스트 조회 중 서버 오류가 발생했습니다"));
        }
    }

    /**
     * 이번달 정산현황 조회
     * 3-1. 이번달 총 정산금액 = (정산완료금액 + 정산처리중금액)
     *      정산완료금액 = COMPLETED 상태
     *      정산처리중금액 = IN_PROGRESS 상태
     */
    @GetMapping("/monthly-status")
    @Operation(
            summary = "이번달 정산현황 조회",
            description = "판매자의 이번달 정산현황(총금액, 완료금액, 처리중금액)을 조회합니다. (Settlement 테이블 데이터만)"
    )
    public ResponseEntity<ApiResponse<MonthlySettlementStatusDto>> getMonthlySettlementStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            log.info("이번달 정산현황 조회 요청 - provider: {}, providerId: {}",
                    userPrincipal.provider(), userPrincipal.providerId());

            MonthlySettlementStatusDto response = settlementService.getMonthlySettlementStatus(userPrincipal);

            log.info("이번달 정산현황 조회 성공 - 총금액: {}, 완료금액: {}, 처리중금액: {}",
                    response.totalMonthlyAmount(), response.completedAmount(), response.inProgressAmount());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("이번달 정산현황 조회 실패 - 판매자를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("이번달 정산현황 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("이번달 정산현황 조회 중 예상치 못한 오류", e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "이번달 정산현황 조회 중 서버 오류가 발생했습니다"));
        }
    }

    /**
     * 월별 정산내역 영수증 조회 (CSV Export용)
     * 3-2. 월별 정산내역 영수증 (나중에 CSV 파일로 export)
     */
    @GetMapping("/monthly-receipt/{targetMonth}")
    @Operation(
            summary = "월별 정산내역 영수증 조회",
            description = "판매자의 특정 월 정산내역 영수증을 조회합니다. (CSV Export용, Settlement 테이블 데이터만)"
    )
    public ResponseEntity<ApiResponse<MonthlySettlementReceiptDto>> getMonthlySettlementReceipt(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "대상 년월 (YYYY-MM 형식)", example = "2024-12")
            @PathVariable String targetMonth) {

        try {
            log.info("월별 정산내역 영수증 조회 요청 - provider: {}, providerId: {}, 대상월: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), targetMonth);

            // 년월 파싱
            YearMonth yearMonth = YearMonth.parse(targetMonth);

            MonthlySettlementReceiptDto response = settlementService.getMonthlySettlementReceipt(
                    userPrincipal, yearMonth);

            log.info("월별 정산내역 영수증 조회 성공 - 대상월: {}, 아이템수: {}",
                    targetMonth, response.items().size());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (DateTimeParseException e) {
            log.warn("월별 정산내역 영수증 조회 실패 - 잘못된 날짜 형식: {}", targetMonth);
            return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, "날짜 형식이 올바르지 않습니다. YYYY-MM 형식으로 입력해주세요"));

        } catch (NoSuchElementException e) {
            log.warn("월별 정산내역 영수증 조회 실패 - 판매자를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("월별 정산내역 영수증 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("월별 정산내역 영수증 조회 중 예상치 못한 오류", e);
            return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "월별 정산내역 영수증 조회 중 서버 오류가 발생했습니다"));
        }
    }
}