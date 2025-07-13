package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.orders.dto.response.BuyerShipmentDetailResponse;
import com.team5.catdogeats.orders.service.BuyerOrderQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * 구매자용 배송 정보 컨트롤러
 * `/v1/buyers/shipments` 경로를 처리하기 위한 별도 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/v1/buyers/shipments")
@RequiredArgsConstructor
public class BuyerShipmentController {

    private final BuyerOrderQueryService buyerOrderQueryService;

    /**
     * 구매자 배송 정보 상세 조회 (물류 서버 연동)
     * API: GET /v1/buyers/shipments/{order-number}
     */
    @GetMapping("/{orderNumber}")
    public ResponseEntity<APIResponse<BuyerShipmentDetailResponse>> getBuyerShipmentDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("orderNumber") String orderNumber) {

        log.info("구매자 배송 정보 상세 조회 요청 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        try {
            BuyerShipmentDetailResponse response = buyerOrderQueryService.getBuyerShipmentDetail(userPrincipal, orderNumber);

            log.info("구매자 배송 정보 상세 조회 성공 - orderNumber: {}, status: {}",
                    orderNumber, response.deliveryStatus());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("구매자 배송 정보 상세 조회 실패 - 주문 없음: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("구매자 배송 정보 상세 조회 실패 - 상태 오류: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("구매자 배송 정보 상세 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("구매자 배송 정보 상세 조회 중 서버 오류", e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "배송 정보 상세 조회 중 서버 오류가 발생했습니다."));
        }
    }
}