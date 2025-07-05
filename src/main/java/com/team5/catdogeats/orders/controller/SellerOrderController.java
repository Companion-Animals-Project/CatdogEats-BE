package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.service.SellerOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * 판매자용 주문 관리 컨트롤러
 * 판매자가 본인이 판매한 상품의 배송지 정보를 조회할 수 있는 API를 제공
 */
@Slf4j
@RestController
@RequestMapping("/v1/sellers/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;

    /**
     * 배송 고객 주소 조회 (판매자)
     * 판매자가 주문번호를 통해 해당 주문의 배송지 정보를 조회합니다.
     * API 명세:
     * - Method: GET
     * - Endpoint: /v1/sellers/orders/{order-number}
     * - 인증: JWT 토큰 필수 (판매자 권한)
     * 보안 정책:
     * - 판매자는 본인이 판매한 상품이 포함된 주문만 조회 가능
     * - 구매자의 민감정보는 제외하고 배송에 필요한 정보만 제공
     * @param userPrincipal JWT에서 추출된 인증된 판매자 정보
     * @param orderNumber 조회할 주문 번호
     * @return 판매자용 주문 상세 정보 (배송지 정보 + 해당 판매자 상품 목록)
     */
    @GetMapping("/{order-number}")
    public ResponseEntity<ApiResponse<SellerOrderDetailResponse>> getSellerOrderDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("order-number") String orderNumber) {

        log.info("배송 고객 주소 조회 요청 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        try {
            SellerOrderDetailResponse response = sellerOrderService.getSellerOrderDetail(userPrincipal, orderNumber);

            log.info("배송 고객 주소 조회 성공 - orderNumber: {}, itemCount: {}, totalAmount: {}원",
                    orderNumber, response.orderItems().size(), response.totalAmount());

            // ApiResponse 래핑 추가
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.ORDER_SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("배송 고객 주소 조회 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            // 구매자용과 동일한 패턴 적용
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("배송 고객 주소 조회 권한 오류 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            // 구매자용과 동일한 패턴 적용
            return ResponseEntity
                    .status(ResponseCode.ACCESS_DENIED.getStatus())
                    .body(ApiResponse.error(ResponseCode.ACCESS_DENIED, e.getMessage()));

        } catch (Exception e) {
            log.error("배송 고객 주소 조회 중 서버 오류 - orderNumber: {}", orderNumber, e);
            // 구매자용과 동일한 패턴 적용
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 상세 조회 중 서버 오류가 발생했습니다."));
        }
    }
}