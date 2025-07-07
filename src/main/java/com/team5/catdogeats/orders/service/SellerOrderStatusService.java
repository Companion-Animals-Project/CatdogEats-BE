package com.team5.catdogeats.orders.service.seller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.request.OrderStatusUpdateRequest;
import com.team5.catdogeats.orders.dto.response.OrderStatusUpdateResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

/**
 * 판매자용 주문 상태 관리 서비스
 * 단일 책임: 주문 상태 변경 및 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderStatusService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;

    /**
     * 주문 상태 변경
     * @param userPrincipal 인증된 판매자 정보
     * @param request 상태 변경 요청
     * @return 상태 변경 결과
     */
    @JpaTransactional
    public OrderStatusUpdateResponse updateOrderStatus(UserPrincipal userPrincipal, OrderStatusUpdateRequest request) {
        try {
            log.debug("주문 상태 변경 시작 - orderNumber: {}, newStatus: {}",
                    request.orderNumber(), request.newStatus());

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 검증
            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(request.orderNumber(), seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            Orders order = shipment.getOrders();
            OrderStatus currentStatus = order.getOrderStatus();

            // 3. 상태 전환 검증
            validateStatusTransition(request, currentStatus);
            validateReasonRequirement(request);

            // 4. 상태 변경
            order.setOrderStatus(request.newStatus());

            // 5. 특별한 상태 변경 처리
            handleSpecialStatusChange(request, shipment);

            // 6. 저장
            orderRepository.save(order);

            log.info("주문 상태 변경 완료 - orderNumber: {}, {} → {}",
                    request.orderNumber(), currentStatus, request.newStatus());

            // 7. 응답 생성
            return createStatusUpdateResponse(request, currentStatus);

        } catch (NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            log.warn("주문 상태 변경 실패 - orderNumber: {}, reason: {}",
                    request.orderNumber(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 상태 변경 중 예상치 못한 오류 발생 - orderNumber: {}",
                    request.orderNumber(), e);
            throw new RuntimeException("주문 상태 변경 중 서버 오류가 발생했습니다", e);
        }
    }

    // ===== Private Helper Methods =====

    private Sellers findSellerByPrincipal(UserPrincipal userPrincipal) {
        Users user = userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));

        return sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("판매자 권한이 없습니다"));
    }

    private void validateStatusTransition(OrderStatusUpdateRequest request, OrderStatus currentStatus) {
        if (!request.isValidStatusTransition(currentStatus)) {
            throw new IllegalArgumentException(
                    String.format("잘못된 상태 전환입니다. %s에서 %s로 변경할 수 없습니다",
                            currentStatus, request.newStatus()));
        }
    }

    private void validateReasonRequirement(OrderStatusUpdateRequest request) {
        if (request.isReasonRequired() &&
                (request.reason() == null || request.reason().trim().isEmpty())) {
            throw new IllegalArgumentException("해당 상태 변경에는 사유가 필수입니다");
        }
    }

    private void handleSpecialStatusChange(OrderStatusUpdateRequest request, Shipments shipment) {
        // 출고 지연 처리
        if (request.isDelayRequest()) {
            log.info("출고 지연 처리 - orderNumber: {}, reason: {}",
                    request.orderNumber(), request.reason());

            // 출고 지연 관련 추가 로직이 필요한 경우 여기에 구현
            // 예: 고객 알림, 예상 출고일 설정 등
        }

        // 주문 취소 처리
        if (request.isCancellationRequest()) {
            log.info("주문 취소 처리 - orderNumber: {}, reason: {}",
                    request.orderNumber(), request.reason());

            // 취소 관련 추가 로직이 필요한 경우 여기에 구현
            // 예: 재고 복원, 결제 취소, 고객 알림 등
        }
    }

    private OrderStatusUpdateResponse createStatusUpdateResponse(
            OrderStatusUpdateRequest request, OrderStatus previousStatus) {

        // 지연 요청인 경우
        if (request.isDelayRequest()) {
            return OrderStatusUpdateResponse.successWithDelay(
                    request.orderNumber(),
                    previousStatus,
                    request.newStatus(),
                    request.reason(),
                    true,
                    request.expectedShipDate()
            );
        }

        // 일반적인 상태 변경
        return OrderStatusUpdateResponse.success(
                request.orderNumber(),
                previousStatus,
                request.newStatus(),
                request.reason()
        );
    }
}