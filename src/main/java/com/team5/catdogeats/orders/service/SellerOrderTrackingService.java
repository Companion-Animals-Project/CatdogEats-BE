package com.team5.catdogeats.orders.service.seller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.request.TrackingNumberRegisterRequest;
import com.team5.catdogeats.orders.dto.response.TrackingNumberRegisterResponse;
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
 * 판매자용 운송장 관리 서비스
 * 단일 책임: 운송장 번호 등록 및 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderTrackingService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;

    /**
     * 운송장 번호 등록
     * @param userPrincipal 인증된 판매자 정보
     * @param request 운송장 등록 요청
     * @return 운송장 등록 결과
     */
    @JpaTransactional
    public TrackingNumberRegisterResponse registerTrackingNumber(
            UserPrincipal userPrincipal,
            TrackingNumberRegisterRequest request) {

        try {
            log.debug("운송장 번호 등록 시작 - orderNumber: {}, courier: {}, trackingNumber: {}",
                    request.orderNumber(), request.getCourierDisplayName(), request.trackingNumber());

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 검증
            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(request.orderNumber(), seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            Orders order = shipment.getOrders();

            // 3. 운송장 등록 가능한 상태인지 검증
            validateTrackingNumberRegistration(order, shipment, request);

            // 4. 운송장 번호 중복 확인
            validateTrackingNumberDuplication(request);

            // 5. 스마트택배 API 검증 (옵션)
            TrackingNumberRegisterResponse.ValidationResult validationResult =
                    validateTrackingNumberWithApi(request);

            // 6. 운송장 정보 저장
            String normalizedTrackingNumber = request.getNormalizedTrackingNumber();
            shipment.setTrackingInfo(
                    request.getCourierDisplayName(),
                    normalizedTrackingNumber
            );

            // 배송 메모 설정
            if (request.hasShipmentMemo()) {
                shipment.setShipmentMemo(request.shipmentMemo());
            }

            // 7. 즉시 배송 시작 처리
            if (request.shouldStartShipmentImmediately()) {
                order.setOrderStatus(OrderStatus.IN_DELIVERY);
            }

            // 8. 저장
            orderRepository.save(order);
            shipmentRepository.save(shipment);

            log.info("운송장 번호 등록 완료 - orderNumber: {}, trackingNumber: {}, newStatus: {}",
                    request.orderNumber(), normalizedTrackingNumber, order.getOrderStatus());

            // 9. 응답 생성
            return TrackingNumberRegisterResponse.successWithValidation(
                    request.orderNumber(),
                    normalizedTrackingNumber,
                    request.courierCompany(),
                    order.getOrderStatus(),
                    shipment.getShippedAt(),
                    validationResult
            );

        } catch (NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            log.warn("운송장 번호 등록 실패 - orderNumber: {}, reason: {}",
                    request.orderNumber(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("운송장 번호 등록 중 예상치 못한 오류 발생 - orderNumber: {}",
                    request.orderNumber(), e);
            throw new RuntimeException("운송장 번호 등록 중 서버 오류가 발생했습니다", e);
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

    private void validateTrackingNumberRegistration(
            Orders order, Shipments shipment, TrackingNumberRegisterRequest request) {

        // 주문 상태 확인
        if (order.getOrderStatus() != OrderStatus.READY_FOR_SHIPMENT) {
            throw new IllegalStateException("배송 준비 완료 상태에서만 운송장을 등록할 수 있습니다");
        }

        // 이미 운송장이 등록된 경우
        if (shipment.isShipped()) {
            throw new IllegalStateException("이미 운송장이 등록된 주문입니다");
        }

        // 운송장 번호 형식 검증
        if (!request.isValidTrackingNumberFormat()) {
            throw new IllegalArgumentException(
                    String.format("%s의 운송장 번호 형식이 올바르지 않습니다",
                            request.getCourierDisplayName()));
        }
    }

    private void validateTrackingNumberDuplication(TrackingNumberRegisterRequest request) {
        String normalizedTrackingNumber = request.getNormalizedTrackingNumber();
        String courierDisplayName = request.getCourierDisplayName();

        if (shipmentRepository.existsByCourierAndTrackingNumber(courierDisplayName, normalizedTrackingNumber)) {
            throw new IllegalStateException("이미 등록된 운송장 번호입니다");
        }
    }

    private TrackingNumberRegisterResponse.ValidationResult validateTrackingNumberWithApi(
            TrackingNumberRegisterRequest request) {

        // API 검증을 수행하지 않는 경우
        if (!request.shouldValidateWithApi()) {
            return TrackingNumberRegisterResponse.ValidationResult.skipped("API 검증을 생략했습니다");
        }

        try {
            // TODO: 스마트택배 API 연동 구현
            // 현재는 기본 형식 검증만 수행하고 성공으로 처리
            if (request.isValidTrackingNumberFormat()) {
                log.debug("운송장 번호 기본 형식 검증 성공 - courier: {}, trackingNumber: {}",
                        request.getCourierDisplayName(), request.trackingNumber());
                return TrackingNumberRegisterResponse.ValidationResult.success("운송장 번호가 유효합니다");
            } else {
                return TrackingNumberRegisterResponse.ValidationResult.failure("운송장 번호 형식이 올바르지 않습니다");
            }

        } catch (Exception e) {
            log.warn("스마트택배 API 검증 중 오류 발생 - courier: {}, trackingNumber: {}, error: {}",
                    request.getCourierDisplayName(), request.trackingNumber(), e.getMessage());

            // API 오류 시에도 등록은 허용하되, 검증 실패로 기록
            return TrackingNumberRegisterResponse.ValidationResult.failure(
                    "API 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}