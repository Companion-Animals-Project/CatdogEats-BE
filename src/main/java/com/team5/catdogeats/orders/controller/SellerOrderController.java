package com.team5.catdogeats.orders.controller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.request.OrderStatusUpdateRequest;
import com.team5.catdogeats.orders.dto.request.TrackingNumberRegisterRequest;
import com.team5.catdogeats.orders.dto.response.*;
import com.team5.catdogeats.orders.service.SellerOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * 판매자용 주문 관리 컨트롤러 (완전 확장)
 * 판매자가 본인이 판매한 상품의 주문/배송 관리를 할 수 있는 모든 API를 제공
 *
 * 내부적으로 기능별 서비스들을 사용하지만,
 * 기존 SellerOrderService 인터페이스를 통해 Facade 패턴으로 접근
 */
@Slf4j
@RestController
@RequestMapping("/v1/sellers/orders")
@RequiredArgsConstructor
public class SellerOrderController {

    private final SellerOrderService sellerOrderService;

    /**
     * 배송 고객 주소 조회 (판매자) - 기존 기능
     * API: GET /v1/sellers/orders/{order-number}
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
                    orderNumber, response.orderItems().size(), response.getTotalAmount());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("배송 고객 주소 조회 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("배송 고객 주소 조회 권한 오류 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ACCESS_DENIED.getStatus())
                    .body(ApiResponse.error(ResponseCode.ACCESS_DENIED, e.getMessage()));

        } catch (Exception e) {
            log.error("배송 고객 주소 조회 중 서버 오류 - orderNumber: {}", orderNumber, e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 상세 조회 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 배송 관리 - 주문 목록 조회 (판매자)
     * API: GET /v1/sellers/orders/list?page={}&sort={}
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<SellerOrderListResponse>> getSellerOrderList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String searchKeyword) {

        log.info("판매자 주문 목록 조회 요청 - provider: {}, providerId: {}, page: {}, size: {}, sort: {}",
                userPrincipal.provider(), userPrincipal.providerId(), page, size, sort);

        try {
            // 페이징 및 정렬 설정
            Pageable pageable = createPageable(page, size, sort);
            SellerOrderListResponse response;

            // 검색 조건에 따른 분기 처리
            if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
                // 검색 처리
                response = sellerOrderService.searchSellerOrders(userPrincipal, searchType, searchKeyword, pageable);
                log.info("판매자 주문 검색 완료 - searchType: {}, keyword: {}, resultCount: {}",
                        searchType, searchKeyword, response.orders().size());

            } else if (status != null && !status.trim().isEmpty()) {
                // 상태 필터링 처리
                OrderStatus orderStatus = parseOrderStatus(status);
                response = sellerOrderService.getSellerOrdersByStatus(userPrincipal, orderStatus, pageable);
                log.info("판매자 주문 상태별 조회 완료 - status: {}, resultCount: {}",
                        status, response.orders().size());

            } else {
                // 전체 목록 조회
                response = sellerOrderService.getSellerOrders(userPrincipal, pageable);
                log.info("판매자 주문 전체 목록 조회 완료 - resultCount: {}", response.orders().size());
            }

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (IllegalArgumentException e) {
            log.warn("판매자 주문 목록 조회 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("판매자 주문 목록 조회 중 서버 오류", e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 목록 조회 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 배송 상태 관리 - 주문 상태 변경 (판매자)
     * API: POST /v1/sellers/orders/status
     */
    @PostMapping("/status")
    public ResponseEntity<ApiResponse<OrderStatusUpdateResponse>> updateOrderStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody OrderStatusUpdateRequest request) {

        log.info("주문 상태 변경 요청 - provider: {}, providerId: {}, orderNumber: {}, newStatus: {}",
                userPrincipal.provider(), userPrincipal.providerId(),
                request.orderNumber(), request.newStatus());

        try {
            OrderStatusUpdateResponse response = sellerOrderService.updateOrderStatus(userPrincipal, request);

            log.info("주문 상태 변경 성공 - orderNumber: {}, {} → {}",
                    request.orderNumber(), response.previousStatus(), response.newStatus());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("주문 상태 변경 실패 - orderNumber: {}, reason: {}", request.orderNumber(), e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("주문 상태 변경 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("주문 상태 변경 실패 - 상태 오류: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_STATE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_STATE, e.getMessage()));

        } catch (Exception e) {
            log.error("주문 상태 변경 중 서버 오류 - orderNumber: {}", request.orderNumber(), e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 상태 변경 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 배송 운송장 등록 (판매자)
     * API: POST /v1/sellers/orders/tracking-number
     */
    @PostMapping("/tracking-number")
    public ResponseEntity<ApiResponse<TrackingNumberRegisterResponse>> registerTrackingNumber(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody TrackingNumberRegisterRequest request) {

        log.info("운송장 번호 등록 요청 - provider: {}, providerId: {}, orderNumber: {}, courier: {}",
                userPrincipal.provider(), userPrincipal.providerId(),
                request.orderNumber(), request.courierCompany());

        try {
            TrackingNumberRegisterResponse response = sellerOrderService.registerTrackingNumber(userPrincipal, request);

            log.info("운송장 번호 등록 성공 - orderNumber: {}, trackingNumber: {}, courier: {}",
                    request.orderNumber(), response.trackingNumber(), response.courierCompany());

            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));

        } catch (NoSuchElementException e) {
            log.warn("운송장 번호 등록 실패 - orderNumber: {}, reason: {}", request.orderNumber(), e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("운송장 번호 등록 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (IllegalStateException e) {
            log.warn("운송장 번호 등록 실패 - 상태 오류: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_STATE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_STATE, e.getMessage()));

        } catch (Exception e) {
            log.error("운송장 번호 등록 중 서버 오류 - orderNumber: {}", request.orderNumber(), e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "운송장 번호 등록 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 주문 목록에서 숨김 처리 (추가 기능)
     * API: POST /v1/sellers/orders/{order-number}/hide
     */
    @PostMapping("/{order-number}/hide")
    public ResponseEntity<ApiResponse<String>> hideOrderFromList(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable("order-number") String orderNumber) {

        log.info("주문 목록 숨김 처리 요청 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        try {
            boolean result = sellerOrderService.hideOrderFromList(userPrincipal, orderNumber);

            if (result) {
                log.info("주문 목록 숨김 처리 성공 - orderNumber: {}", orderNumber);
                return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, "주문이 목록에서 숨겨졌습니다."));
            } else {
                log.warn("주문 목록 숨김 처리 실패 - 이미 숨겨진 주문: {}", orderNumber);
                return ResponseEntity
                        .status(ResponseCode.INVALID_STATE.getStatus())
                        .body(ApiResponse.error(ResponseCode.INVALID_STATE, "이미 숨겨진 주문입니다."));
            }

        } catch (NoSuchElementException e) {
            log.warn("주문 목록 숨김 처리 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.ENTITY_NOT_FOUND.getStatus())
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));

        } catch (IllegalArgumentException e) {
            log.warn("주문 목록 숨김 처리 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity
                    .status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));

        } catch (Exception e) {
            log.error("주문 목록 숨김 처리 중 서버 오류 - orderNumber: {}", orderNumber, e);
            return ResponseEntity
                    .status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "주문 숨김 처리 중 서버 오류가 발생했습니다."));
        }
    }

    // ===== Private Helper Methods =====

    /**
     * 페이징 객체 생성
     */
    private Pageable createPageable(int page, int size, String sort) {
        // 페이지 크기 제한
        size = Math.min(Math.max(size, 1), 100);

        // 정렬 파싱
        String[] sortParts = sort.split(",");
        String property = sortParts.length > 0 ? sortParts[0] : "createdAt";
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        return PageRequest.of(page, size, Sort.by(direction, property));
    }

    /**
     * 주문 상태 파싱
     */
    private OrderStatus parseOrderStatus(String status) {
        try {
            return OrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("지원하지 않는 주문 상태입니다: " + status);
        }
    }
}