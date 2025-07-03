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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 판매자용 주문 관리 서비스 구현체
 * 판매자가 본인이 판매한 상품의 배송지 정보를 안전하게 조회할 수 있도록 하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerOrderServiceImpl implements SellerOrderService {

    private final ShipmentRepository shipmentRepository;
    private final UserRepository userRepository;
    private final SellersRepository sellersRepository;

    /**
     * 판매자용 주문 상세 조회 (배송지 정보 포함)
     * 판매자 권한 검증 후 해당 판매자의 상품만 필터링하여 반환합니다.
     */
    @Override
    public SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, Long orderNumber) {
        log.info("판매자용 주문 상세 조회 요청 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        try {
            // 1. 판매자 검증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);

            // 2. 주문번호로 배송정보 조회 (판매자 권한 검증 포함)
            Shipments shipment = shipmentRepository.findShippingInfoByOrderNumberAndSeller(
                            orderNumber, seller.getUserId())
                    .orElseThrow(() -> new NoSuchElementException(
                            "주문을 찾을 수 없거나 접근 권한이 없습니다"));

            // 3. 배송지 정보 생성
            SellerOrderDetailResponse.RecipientInfo recipientInfo = createRecipientInfo(shipment);

            // 4. 해당 판매자의 주문 상품만 필터링
            List<SellerOrderDetailResponse.SellerOrderItem> sellerOrderItems =
                    filterSellerOrderItems(shipment, seller.getUserId());

            // 5. 해당 판매자 상품들의 총 금액 계산
            Long totalAmount = calculateSellerTotalAmount(sellerOrderItems);

            // 6. 응답 생성
            SellerOrderDetailResponse response = SellerOrderDetailResponse.success(
                    shipment.getOrders().getOrderNumber(),
                    shipment.getOrders().getCreatedAt(),
                    shipment.getOrders().getOrderStatus(),
                    recipientInfo,
                    sellerOrderItems,
                    totalAmount
            );

            log.info("판매자용 주문 상세 조회 완료 - orderNumber: {}, sellerId: {}, itemCount: {}, totalAmount: {}원",
                    orderNumber, seller.getUserId(), sellerOrderItems.size(), totalAmount);

            return response;

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("판매자용 주문 상세 조회 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("판매자용 주문 상세 조회 중 예상치 못한 오류 발생 - orderNumber: {}", orderNumber, e);
            throw new RuntimeException("주문 상세 조회 중 서버 오류가 발생했습니다", e);
        }
    }

    /**
     * UserPrincipal로 판매자 조회 및 검증
     */
    private Sellers findSellerByPrincipal(UserPrincipal userPrincipal) {
        // 1. UserPrincipal로 Users 조회
        Users user = userRepository.findByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다"));

        // 2. Users로 Sellers 조회
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
     * 해당 판매자의 주문 상품만 필터링
     */
    private List<SellerOrderDetailResponse.SellerOrderItem> filterSellerOrderItems(
            Shipments shipment, String sellerId) {

        return shipment.getOrders().getOrderItems().stream()
                .filter(orderItem -> sellerId.equals(orderItem.getProducts().getSeller().getUserId()))
                .map(this::convertToSellerOrderItem)
                .toList();
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