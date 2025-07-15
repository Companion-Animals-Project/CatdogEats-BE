package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.orders.dto.request.OrderStatusUpdateRequest;
import com.team5.catdogeats.orders.dto.request.TrackingNumberRegisterRequest;
import com.team5.catdogeats.orders.dto.response.OrderStatusUpdateResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.dto.response.SellerOrderListResponse;
import com.team5.catdogeats.orders.dto.response.ShipmentSyncResponse;
import com.team5.catdogeats.orders.dto.response.TrackingNumberRegisterResponse;
import com.team5.catdogeats.orders.service.SellerOrderCommandService;
import com.team5.catdogeats.orders.service.SellerOrderService;
import com.team5.catdogeats.orders.dto.request.OrderDeleteRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * 판매자용 주문 관리 컨트롤러
 * 판매자가 본인이 판매한 상품의 주문을 관리할 수 있는 기능들을 제공합니다.
 * 포함 기능:
 * - 주문 목록 조회 (페이징)
 * - 주문 상세 조회 (배송지 정보 포함)
 * - 주문 상태 변경 (배송 상태 관리)
 * - 운송장 번호 등록 (배송 운송장 등록)
 * - 배송 상태 동기화 (물류 서버 연동)
 * - 주문 내역 삭제 (목록 숨김 처리)
 */
@Slf4j
@RestController
@RequestMapping("/v1/sellers/orders")
@RequiredArgsConstructor
@Tag(name = "Seller-Order-Management", description = "판매자가 주문/배송 관리 페이지에서 할 수 있는 기능들")
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;
    private final SellerOrderCommandService sellerOrderCommandService;

    /**
     * 배송 관리 (판매자) - 주문 목록 조회
     * API: GET /v1/sellers/orders/list?page={}&sort={}
     */
    @Operation(summary = "주문 목록 조회(판매자)", description = "판매자가 결제 완료된 주문 목록을 확인 할 수 있는 api")
    @GetMapping("/list")
    public ResponseEntity<APIResponse<SellerOrderListResponse>> getSellerOrders(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("판매자 주문 목록 조회 요청 - provider: {}, providerId: {}, page: {}, size: {}",
                userPrincipal.provider(), userPrincipal.providerId(), pageable.getPageNumber(), pageable.getPageSize());

        try {
            SellerOrderListResponse response = sellerOrderService.getSellerOrders(userPrincipal, pageable);

            log.info("판매자 주문 목록 조회 성공 - provider: {}, providerId: {}, 총 주문수: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), response.totalElements());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("판매자 주문 목록 조회 실패 - 사용자 없음: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("판매자 주문 목록 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("판매자 주문 목록 조회 중 서버 오류", e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 목록 조회 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 배송 고객 주소 조회 (판매자)
     * API: GET /v1/sellers/orders/{order-number}
     */
    @Operation(summary = "구매자 정보 조회(판매자)", description = "판매자가 주문한 구매자의 정보에 대해 조회하는 api")
    @GetMapping("/{order-number}")
    public ResponseEntity<APIResponse<SellerOrderDetailResponse>> getSellerOrderDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("order-number") String orderNumber) {

        log.info("판매자 주문 상세 조회 요청 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        try {
            SellerOrderDetailResponse response = sellerOrderService.getSellerOrderDetail(userPrincipal, orderNumber);

            log.info("판매자 주문 상세 조회 성공 - orderNumber: {}, 상품수: {}",
                    orderNumber, response.orderItems().size());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("판매자 주문 상세 조회 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("판매자 주문 상세 조회 실패 - 권한 없음: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ACCESS_DENIED.getStatus())
                    .body(APIResponse.error(ResponseCode.ACCESS_DENIED, e.getMessage()));

        } catch (Exception e) {
            log.error("판매자 주문 상세 조회 중 서버 오류 - orderNumber: {}", orderNumber, e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 조회 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 배송 상태 관리 (판매자)
     * API: POST /v1/sellers/orders/status
     */
    @Operation(summary = "배송 상태 관리", description = "판매자가 주문의 배송 상태에 대해 관리 할 수 있는 api" +
            "PAYMENT_COMPLETED(결제 완료), PREPARING (상품준비중), READY_FOR_SHIPMENT (배송준비완료)")
    @PostMapping("/status")
    public ResponseEntity<APIResponse<OrderStatusUpdateResponse>> updateOrderStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OrderStatusUpdateRequest request) {

        log.info("주문 상태 변경 요청 - provider: {}, providerId: {}, orderNumber: {}, newStatus: {}",
                userPrincipal.provider(), userPrincipal.providerId(),
                request.orderNumber(), request.newStatus());

        try {
            OrderStatusUpdateResponse response = sellerOrderCommandService.updateOrderStatus(userPrincipal, request);

            log.info("주문 상태 변경 성공 - orderNumber: {}, {} -> {}",
                    request.orderNumber(), response.previousStatus(), response.currentStatus());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("주문 상태 변경 실패 - orderNumber: {}, reason: {}", request.orderNumber(), e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("주문 상태 변경 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("주문 상태 변경 실패 - 상태 오류: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("주문 상태 변경 중 서버 오류 - orderNumber: {}", request.orderNumber(), e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 상태 변경 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 배송 운송장 등록 (판매자)
     * API: POST /v1/sellers/orders/tracking-number
     */
    @Operation(summary = "배송 운송장 등록", description = "판매자가 물류 서버에서 받은 운송장 번호를 등록하여 주문 상태를 배송중으로 바꾸는 api")
    @PostMapping("/tracking-number")
    public ResponseEntity<APIResponse<TrackingNumberRegisterResponse>> registerTrackingNumber(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody TrackingNumberRegisterRequest request) {

        log.info("운송장 번호 등록 요청 - provider: {}, providerId: {}, orderNumber: {}, courier: {}",
                userPrincipal.provider(), userPrincipal.providerId(),
                request.orderNumber(), request.courierCompany());

        try {
            TrackingNumberRegisterResponse response = sellerOrderCommandService.registerTrackingNumber(userPrincipal, request);

            log.info("운송장 번호 등록 성공 - orderNumber: {}, trackingNumber: {}, courier: {}",
                    request.orderNumber(), response.trackingNumber(), response.courierCompany());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("운송장 번호 등록 실패 - orderNumber: {}, reason: {}", request.orderNumber(), e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("운송장 번호 등록 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("운송장 번호 등록 실패 - 상태 오류: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("운송장 번호 등록 중 서버 오류 - orderNumber: {}", request.orderNumber(), e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "운송장 번호 등록 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 전체 배송 상태 동기화 (판매자)
     * API: POST /v1/sellers/orders/sync-shipment-status
     * 판매자의 모든 배송 중 주문에 대해 물류 서버에서 상태를 조회하고 배송완료 주문을 자동 업데이트
     */
    @Operation(summary = "배송중인 주문 상태 동기화", description = "물류서버로 부터 배송중인 주문에 대해 최신 상태를 업데이트 받을 수 있는 api")
    @PostMapping("/sync-shipment-status")
    public ResponseEntity<APIResponse<ShipmentSyncResponse>> syncAllShipmentStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        log.info("전체 배송 상태 동기화 요청 - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        try {
            ShipmentSyncResponse response = sellerOrderCommandService.syncAllShipmentStatus(userPrincipal);

            log.info("전체 배송 상태 동기화 완료 - provider: {}, providerId: {}, 결과: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), response.getSummary());

            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("배송 상태 동기화 실패 - 사용자 없음: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("배송 상태 동기화 실패 - 권한 없음: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("배송 상태 동기화 중 서버 오류", e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "배송 상태 동기화 중 서버 오류가 발생했습니다."));
        }
    }
    /**
     * 주문 내역 삭제 (판매자)
     * API: DELETE /v1/sellers/orders
     */
    @Operation(summary = "주문 내역 삭제(판매자)", description = "배송완료나 취소된 주문을 관리 차원에서 삭제하는 api")
    @DeleteMapping
    public ResponseEntity<APIResponse<String>> deleteOrder(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestBody @Valid OrderDeleteRequest request) {

        log.info("주문 삭제 요청 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), request.orderNumber());

        try {
            boolean result = sellerOrderCommandService.deleteOrder(userPrincipal, request.orderNumber());

            if (result) {
                log.info("주문 삭제 성공 - orderNumber: {}", request.orderNumber());
                return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, "주문이 성공적으로 삭제되었습니다."));
            } else {
                log.warn("주문 삭제 실패 - orderNumber: {}, 알 수 없는 오류", request.orderNumber());
                return ResponseEntity
                        .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                        .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 삭제에 실패했습니다."));
            }

        } catch (NoSuchElementException e) {
            log.warn("주문 삭제 실패 - orderNumber: {}, reason: {}", request.orderNumber(), e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(APIResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("주문 삭제 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(APIResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("주문 삭제 중 서버 오류 - orderNumber: {}", request.orderNumber(), e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 삭제 중 서버 오류가 발생했습니다."));
        }
    }
}