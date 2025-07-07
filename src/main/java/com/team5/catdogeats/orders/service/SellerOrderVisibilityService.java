package com.team5.catdogeats.orders.service.seller;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
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
 * 판매자용 주문 목록 숨김 관리 서비스
 * 단일 책임: 주문 목록에서 숨김/표시 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerOrderVisibilityService {

    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final ShipmentRepository shipmentRepository;

    /**
     * 주문 목록에서 숨김 처리
     * @param userPrincipal 인증된 판매자 정보
     * @param orderNumber 숨김 처리할 주문 번호
     * @return 숨김 처리 성공 여부
     */
    @JpaTransactional
    public boolean hideOrderFromList(UserPrincipal userPrincipal, String orderNumber) {
        try {
            log.debug("주문 목록 숨김 처리 시작 - provider: {}, providerId: {}, orderNumber: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 검증
            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(orderNumber, seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            Orders order = shipment.getOrders();

            // 3. 숨김 처리 가능한 상태인지 확인
            validateHideOrderCondition(order.getOrderStatus(), shipment);

            // 4. 이미 숨겨진 주문인지 확인
            if (shipment.isHiddenBySeller()) {
                log.warn("이미 숨김 처리된 주문 - orderNumber: {}", orderNumber);
                return false;
            }

            // 5. 숨김 처리
            shipment.hideFromSellerList();

            // 6. 저장
            shipmentRepository.save(shipment);

            log.info("주문 목록 숨김 처리 완료 - orderNumber: {}", orderNumber);
            return true;

        } catch (NoSuchElementException | IllegalArgumentException | IllegalStateException e) {
            log.warn("주문 목록 숨김 처리 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 목록 숨김 처리 중 예상치 못한 오류 발생 - orderNumber: {}", orderNumber, e);
            throw new RuntimeException("주문 숨김 처리 중 서버 오류가 발생했습니다", e);
        }
    }

    /**
     * 주문 목록에서 숨김 해제 처리
     * @param userPrincipal 인증된 판매자 정보
     * @param orderNumber 숨김 해제할 주문 번호
     * @return 숨김 해제 성공 여부
     */
    @JpaTransactional
    public boolean showOrderInList(UserPrincipal userPrincipal, String orderNumber) {
        try {
            log.debug("주문 목록 숨김 해제 시작 - provider: {}, providerId: {}, orderNumber: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 검증
            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(orderNumber, seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            // 3. 숨겨진 주문인지 확인
            if (!shipment.isHiddenBySeller()) {
                log.warn("이미 표시 중인 주문 - orderNumber: {}", orderNumber);
                return false;
            }

            // 4. 숨김 해제
            shipment.showInSellerList();

            // 5. 저장
            shipmentRepository.save(shipment);

            log.info("주문 목록 숨김 해제 완료 - orderNumber: {}", orderNumber);
            return true;

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("주문 목록 숨김 해제 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("주문 목록 숨김 해제 중 예상치 못한 오류 발생 - orderNumber: {}", orderNumber, e);
            throw new RuntimeException("주문 숨김 해제 중 서버 오류가 발생했습니다", e);
        }
    }

    /**
     * 주문 숨김 상태 조회
     * @param userPrincipal 인증된 판매자 정보
     * @param orderNumber 조회할 주문 번호
     * @return 숨김 상태 (true: 숨김, false: 표시)
     */
    @JpaTransactional(readOnly = true)
    public boolean isOrderHidden(UserPrincipal userPrincipal, String orderNumber) {
        try {
            log.debug("주문 숨김 상태 조회 - provider: {}, providerId: {}, orderNumber: {}",
                    userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

            // 1. 판매자 인증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문 조회 및 권한 검증
            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(orderNumber, seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            return shipment.isHiddenBySeller();

        } catch (Exception e) {
            log.error("주문 숨김 상태 조회 중 오류 발생 - orderNumber: {}", orderNumber, e);
            throw new RuntimeException("주문 숨김 상태 조회 중 서버 오류가 발생했습니다", e);
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

    private void validateHideOrderCondition(OrderStatus orderStatus, Shipments shipment) {
        // 배송 완료 또는 취소된 주문만 숨김 처리 가능
        if (orderStatus != OrderStatus.DELIVERED && orderStatus != OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("배송 완료 또는 취소된 주문만 목록에서 숨길 수 있습니다");
        }
    }
}