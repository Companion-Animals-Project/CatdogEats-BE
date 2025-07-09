package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.users.domain.dto.SellerDashboardResponseDTO;
import com.team5.catdogeats.users.service.SellerDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

/**
 * 판매자 대시보드 컨트롤러
 * 판매자의 대시보드에 필요한 통계 데이터를 제공합니다.
 * 실시간 주문 데이터를 기반으로 한 대시보드 정보를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "판매자 대시보드", description = "판매자 대시보드 관련 API")
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;

    /**
     * 판매자 대시보드 데이터 조회
     *
     * @param userPrincipal JWT에서 추출된 인증된 사용자 정보
     * @return 대시보드 데이터 (오늘 통계, 주간 매출, 상품 순위)
     */
    @GetMapping("/dashboard")
    @Operation(
            summary = "판매자 대시보드 데이터 조회",
            description = """
                    판매자의 대시보드에 필요한 모든 통계 데이터를 조회합니다.
                    
                    포함되는 데이터:
                    - 오늘 주문 수 및 매출액
                    - 주간 매출 동향 (이번 주 7일간 일별 매출)
                    - 이번 달 상품 매출 순위 (TOP 10)
                    
                    실시간 주문 데이터를 기반으로 하여 정산 완료를 기다리지 않고 즉시 반영됩니다.
                    취소/환불/숨김 처리된 주문은 제외됩니다.
                    
                    판매자 권한(ROLE_SELLER)이 필요합니다.
                    """
    )
    public ResponseEntity<ApiResponse<SellerDashboardResponseDTO>> getDashboardData(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("판매자 대시보드 조회 요청 - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        try {
            SellerDashboardResponseDTO response = sellerDashboardService.getDashboardData(userPrincipal);

            log.info("판매자 대시보드 조회 성공 - provider: {}, providerId: {}, 오늘주문: {}건, 오늘매출: {}원",
                    userPrincipal.provider(), userPrincipal.providerId(),
                    response.todayStats().todayOrderCount(),
                    response.todayStats().todayTotalSales());

            return ResponseEntity.ok(
                    ApiResponse.success(ResponseCode.SUCCESS, response)
            );

        } catch (NoSuchElementException e) {
            log.warn("판매자 대시보드 조회 실패 - 판매자 정보 없음: provider={}, providerId={}, reason={}",
                    userPrincipal.provider(), userPrincipal.providerId(), e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("판매자 대시보드 조회 실패 - 잘못된 요청: provider={}, providerId={}, reason={}",
                    userPrincipal.provider(), userPrincipal.providerId(), e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("판매자 대시보드 조회 중 내부 오류 발생 - provider={}, providerId={}",
                    userPrincipal.provider(), userPrincipal.providerId(), e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "대시보드 데이터 조회 중 서버 오류가 발생했습니다."));
        }
    }
}