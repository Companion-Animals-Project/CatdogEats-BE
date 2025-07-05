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
import java.util.Optional;

/**
 * 판매자용 주문 관리 서비스 구현체
 * 판매자가 본인이 판매한 상품의 배송지 정보를 안전하게 조회할 수 있도록 하는 서비스입니다.
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
     * 판매자용 주문 상세 조회 (배송지 정보 포함)
     * 판매자 권한 검증 후 해당 판매자의 상품만 필터링하여 반환합니다.
     */
    @Override
    public SellerOrderDetailResponse getSellerOrderDetail(UserPrincipal userPrincipal, String orderNumber) {
        log.info("판매자용 주문 상세 조회 요청 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        try {
            // 1. 판매자 검증 및 조회
            Sellers seller = findSellerByPrincipal(userPrincipal);
            log.debug("판매자 인증 성공 - sellerId: {}, vendorName: {}", seller.getUserId(), seller.getVendorName());

            // 2. 단계별 디버깅: 주문 존재 여부 확인
            log.debug("1단계: 주문 존재 여부 확인 - orderNumber: {}", orderNumber);
            Optional<Shipments> shipmentOpt = shipmentRepository.findByOrderNumber(orderNumber);
            if (shipmentOpt.isEmpty()) {
                log.warn("주문 번호에 해당하는 배송정보가 없습니다 - orderNumber: {}", orderNumber);
                throw new NoSuchElementException("주문을 찾을 수 없습니다");
            }

            Shipments shipment = shipmentOpt.get();
            log.debug("배송정보 조회 성공 - shipmentId: {}, orderId: {}",
                    shipment.getId(), shipment.getOrders().getId());

            // 3. 판매자 권한 확인: 해당 주문에 판매자의 상품이 있는지 확인
            log.debug("2단계: 판매자 권한 확인 - sellerId: {}", seller.getUserId());
            List<OrderItems> sellerOrderItems = shipment.getOrders().getOrderItems().stream()
                    .filter(orderItem -> {
                        String itemSellerId = orderItem.getProducts().getSeller().getUserId();
                        log.debug("주문상품 확인 - productId: {}, productSellerId: {}, targetSellerId: {}",
                                orderItem.getProducts().getId(), itemSellerId, seller.getUserId());
                        return seller.getUserId().equals(itemSellerId);
                    })
                    .toList();

            if (sellerOrderItems.isEmpty()) {
                log.warn("해당 주문에 판매자의 상품이 없습니다 - orderNumber: {}, sellerId: {}",
                        orderNumber, seller.getUserId());
                throw new NoSuchElementException("접근 권한이 없습니다");
            }

            log.debug("판매자 권한 확인 성공 - 판매자 상품 수: {}", sellerOrderItems.size());

            // 4. 배송지 정보 생성
            SellerOrderDetailResponse.RecipientInfo recipientInfo = createRecipientInfo(shipment);

            // 5. 판매자 주문 상품 변환
            List<SellerOrderDetailResponse.SellerOrderItem> sellerOrderItemDTOs =
                    sellerOrderItems.stream()
                            .map(this::convertToSellerOrderItem)
                            .toList();

            // 6. 총 금액 계산
            Long totalAmount = calculateSellerTotalAmount(sellerOrderItemDTOs);

            // 7. 응답 생성
            SellerOrderDetailResponse response = SellerOrderDetailResponse.success(
                    shipment.getOrders().getOrderNumber(),
                    shipment.getOrders().getCreatedAt(),
                    shipment.getOrders().getOrderStatus(),
                    recipientInfo,
                    sellerOrderItemDTOs,
                    totalAmount
            );

            log.info("판매자용 주문 상세 조회 완료 - orderNumber: {}, sellerId: {}, itemCount: {}, totalAmount: {}원",
                    orderNumber, seller.getUserId(), sellerOrderItemDTOs.size(), totalAmount);

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