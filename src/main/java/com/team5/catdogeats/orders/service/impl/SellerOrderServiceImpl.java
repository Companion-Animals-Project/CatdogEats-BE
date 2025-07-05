package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.orders.dto.response.SellerOrderDetailResponse;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.SellerOrderService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.team5.catdogeats.global.annotation.JpaTransactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 판매자용 주문 관리 서비스 구현체
 * 판매자가 본인이 판매한 상품의 배송지 정보를 안전하게 조회할 수 있도록 하는 서비스입니다.
 * DB 레벨에서 권한 검증을 수행하여 성능과 보안을 향상시켰습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@JpaTransactional(readOnly = true)
public class SellerOrderServiceImpl implements SellerOrderService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final SellersRepository sellersRepository;

    /**
     * 판매자용 주문 상세 조회 (배송지 정보 포함) - Repository 최적화 버전
     * DB 레벨에서 권한 검증까지 완료하여 성능과 보안을 향상시켰습니다.
     */
    @Override
    public SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, String orderNumber) {
        log.info("판매자용 주문 상세 조회 요청 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        try {
            // 1. 판매자 검증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);
            log.debug("판매자 인증 성공 - sellerId: {}, vendorName: {}", seller.getUserId(), seller.getVendorName());

            // 2. 최적화된 Repository 메서드 활용 (권한 검증까지 DB에서 처리)
            Shipments shipment = shipmentRepository
                    .findShippingInfoByOrderNumberAndSeller(orderNumber, seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            log.debug("배송정보 및 권한 검증 완료 - shipmentId: {}, orderId: {}",
                    shipment.getId(), shipment.getOrders().getId());

            // 3. 이제 해당 판매자의 상품만 이미 필터링된 상태
            List<OrderItems> sellerOrderItems = shipment.getOrders().getOrderItems();

            // 4. DTO 변환 및 응답 생성
            return buildSellerOrderDetailResponse(shipment, sellerOrderItems, seller.getUserId());

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("판매자용 주문 상세 조회 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("판매자용 주문 상세 조회 중 예상치 못한 오류 발생 - orderNumber: {}", orderNumber, e);
            throw new RuntimeException("주문 상세 조회 중 서버 오류가 발생했습니다", e);
        }
    }

    /**
     * 응답 생성 로직 분리
     */
    private SellerOrderDetailResponse buildSellerOrderDetailResponse(
            Shipments shipment,
            List<OrderItems> sellerOrderItems,
            String sellerId) {

        // 배송지 정보 생성
        SellerOrderDetailResponse.RecipientInfo recipientInfo = createRecipientInfo(shipment);

        // 판매자 주문 상품 변환
        List<SellerOrderDetailResponse.SellerOrderItem> sellerOrderItemDTOs =
                sellerOrderItems.stream()
                        .map(this::convertToSellerOrderItem)
                        .toList();

        // 총 금액 계산
        Long totalAmount = calculateSellerTotalAmount(sellerOrderItemDTOs);

        log.info("판매자용 주문 상세 조회 완료 - orderNumber: {}, sellerId: {}, itemCount: {}, totalAmount: {}원",
                shipment.getOrders().getOrderNumber(), sellerId, sellerOrderItemDTOs.size(), totalAmount);

        return SellerOrderDetailResponse.success(
                shipment.getOrders().getOrderNumber(),
                shipment.getOrders().getCreatedAt(),
                shipment.getOrders().getOrderStatus(),
                recipientInfo,
                sellerOrderItemDTOs,
                totalAmount
        );
    }

    /**
     * UserPrincipal로 판매자 조회 및 검증
     */
    private Sellers findSellerByPrincipal(UserPrincipal userPrincipal) {
        // 1. Users 조회
        Users user = userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));

        // 2. Sellers 조회
        return sellersRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("판매자 권한이 없습니다"));
    }

    /**
     * 배송지 정보 생성
     */
    private SellerOrderDetailResponse.RecipientInfo createRecipientInfo(Shipments shipment) {
        return SellerOrderDetailResponse.RecipientInfo.builder()
                .recipientName(shipment.getRecipientName())
                .recipientPhone(shipment.getRecipientPhone())
                .postalCode(shipment.getPostalCode())
                .streetAddress(shipment.getShippingAddress())
                .detailAddress(shipment.getDetailAddress())
                .deliveryNote(shipment.getDeliveryNote() != null ?
                        shipment.getDeliveryNote() : "배송 요청사항 없음")
                .build();
    }

    /**
     * OrderItems를 SellerOrderItem으로 변환
     */
    private SellerOrderDetailResponse.SellerOrderItem convertToSellerOrderItem(OrderItems orderItem) {
        return SellerOrderDetailResponse.SellerOrderItem.builder()
                .productId(orderItem.getProducts().getId())
                .productName(orderItem.getProducts().getTitle())
                .unitPrice(orderItem.getPrice())
                .quantity(orderItem.getQuantity())
                .itemTotalPrice(orderItem.getPrice() * orderItem.getQuantity())
                .build();
    }

    /**
     * 해당 판매자 상품들의 총 금액 계산
     */
    private Long calculateSellerTotalAmount(List<SellerOrderDetailResponse.SellerOrderItem> sellerOrderItems) {
        return sellerOrderItems.stream()
                .mapToLong(SellerOrderDetailResponse.SellerOrderItem::itemTotalPrice)
                .sum();
    }
}