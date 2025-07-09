package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.orders.dto.request.OrderCreateRequest;
import com.team5.catdogeats.orders.dto.request.OrderDeleteRequest;
import com.team5.catdogeats.orders.dto.response.OrderCreateResponse;
import com.team5.catdogeats.orders.dto.response.OrderDeleteResponse;
import com.team5.catdogeats.orders.dto.response.OrderDetailResponse;
import com.team5.catdogeats.orders.service.OrderCreateService;
import com.team5.catdogeats.orders.service.OrderDeleteService;
import com.team5.catdogeats.orders.service.OrderDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.NoSuchElementException;

/**
 * 구매자 주문 관리 컨트롤러 (주문 삭제 기능 추가)
 * JWT 인증을 통한 사용자 식별로 보안성을 강화했습니다.
 * ApiResponse 컨벤션을 적용하여 일관된 응답 형식을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/v1/buyers/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCreateService orderCreateService;
    private final OrderDetailService orderDetailService;
    private final OrderDeleteService orderDeleteService;

    /**
     * 주문 생성 (구매자) - 기존 메서드 유지
     */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderCreateResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OrderCreateRequest request) {

        try {
            log.info("주문 생성 요청: userProvider={}, providerId={}, 상품 개수={}",
                    userPrincipal.provider(), userPrincipal.providerId(), request.getOrderItems().size());

            OrderCreateResponse response = orderCreateService.createOrderByUserPrincipal(userPrincipal, request);

            log.info("주문 생성 성공 (재고 차감 완료): orderId={}, orderNumber={}",
                    response.getOrderId(), response.getOrderNumber());

            return ResponseEntity
                    .created(URI.create("/v1/buyers/orders/" + response.getOrderNumber()))
                    .body(ApiResponse.success(ResponseCode.CREATED, response));

        } catch (NoSuchElementException e) {
            log.warn("주문 생성 실패 - 리소스를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("주문 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("주문 생성 실패 - 재고 차감 실패: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));

        } catch (Exception e) {
            log.error("주문 생성 중 내부 오류 발생", e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 생성 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 주문 상세 조회 (구매자) - 기존 메서드 유지
     */
    @GetMapping("/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> getOrderDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("orderNumber") String orderNumber) {

        try {
            log.info("주문 상세 조회 요청 - provider: {}, providerId: {}, orderNumber: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

            OrderDetailResponse response = orderDetailService.getOrderDetail(userPrincipal, orderNumber);

            log.info("주문 상세 조회 성공 - orderNumber: {}, orderId: {}",
                    orderNumber, response.orderId());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("주문 상세 조회 실패 - 리소스를 찾을 수 없음: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("주문 상세 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("주문 상세 조회 중 내부 오류 발생", e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 상세 조회 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 주문 내역 삭제 (구매자) - 논리적 삭제 방식
     * 주문 상세 페이지에서 호출되는 단일 주문 삭제 기능입니다.
     *
     * @param userPrincipal JWT에서 추출된 인증된 사용자 정보
     * @param request 삭제할 주문 정보 (orderNumber 포함)
     * @return 삭제 처리 결과
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<OrderDeleteResponse>> deleteOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid OrderDeleteRequest request) {

        try {
            log.info("주문 내역 삭제 요청 - provider: {}, providerId: {}, orderNumber: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), request.orderNumber());

            OrderDeleteResponse response = orderDeleteService.deleteOrder(userPrincipal, request.orderNumber());

            if (response.success()) {
                log.info("주문 내역 삭제 성공 - orderNumber: {}, orderId: {}",
                        response.orderNumber(), response.orderId());

                return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
            } else {
                log.warn("주문 내역 삭제 실패 - orderNumber: {}, reason: {}",
                        response.orderNumber(), response.message());

                return ResponseEntity
                        .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                        .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, response.message()));
            }

        } catch (Exception e) {
            log.error("주문 내역 삭제 중 내부 오류 발생 - orderNumber: {}", request.orderNumber(), e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 내역 삭제 중 서버 오류가 발생했습니다."));
        }
    }
}