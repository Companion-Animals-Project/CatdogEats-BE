package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.Shipments;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.response.OrderDetailResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.repository.ShipmentRepository;
import com.team5.catdogeats.orders.service.OrderDetailService;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDetailServiceImpl implements OrderDetailService {
    private final OrderRepository orderRepository;
    private final BuyerRepository buyerRepository;
    private final ShipmentRepository shipmentRepository;

    @Override
    @JpaTransactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(UserPrincipal userPrincipal, String orderNumber) {
        log.info("주문 상세 조회 시작 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        Buyers buyer = findUserByPrincipal(userPrincipal);
        Orders order = orderRepository.findOrderDetailByUserAndOrderNumber(buyer, orderNumber)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다."));

        if (order.getOrderStatus() == OrderStatus.PAYMENT_PENDING) {
            log.warn("결제가 완료되지 않은 주문에 대한 상세 조회 시도: orderNumber={}", orderNumber);
            throw new IllegalStateException("결제가 완료되지 않은 주문은 조회할 수 없습니다.");
        }

        return getOrderDetailFromEntities(order);
    }

    private OrderDetailResponse getOrderDetailFromEntities(Orders order) {
        Shipments shipment = shipmentRepository.findByOrders(order).orElse(null);

        List<OrderDetailResponse.OrderItemDetail> orderItemDetails = order.getOrderItems().stream()
                .map(orderItem -> new OrderDetailResponse.OrderItemDetail(
                        orderItem.getId(),
                        orderItem.getProducts().getId(),
                        orderItem.getProducts().getTitle(),
                        orderItem.getQuantity(),
                        orderItem.getPrice(),
                        orderItem.getPrice() * orderItem.getQuantity()
                )).toList();

        Long totalProductPrice = orderItemDetails.stream()
                .mapToLong(OrderDetailResponse.OrderItemDetail::totalPrice)
                .sum();

        Long discountAmount = order.getTotalDiscountAmount() != null ? order.getTotalDiscountAmount() : 0L;
        Long deliveryFee = order.getTotalDeliveryFee() != null ? order.getTotalDeliveryFee() : 0L;

        OrderDetailResponse.RecipientInfo recipientInfo = createRecipientInfoFromShipment(shipment);
        OrderDetailResponse.PaymentInfo paymentInfo = OrderDetailResponse.PaymentInfo.of(
                totalProductPrice, discountAmount, deliveryFee);

        return new OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getCreatedAt().toLocalDateTime(),
                order.getOrderStatus(),
                recipientInfo,
                paymentInfo,
                orderItemDetails
        );
    }

    private Buyers findUserByPrincipal(UserPrincipal userPrincipal) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("구매자를 찾을 수 없거나 권한이 없습니다"));
        return BuyerDTO.toEntity(buyerDTO);
    }

    private OrderDetailResponse.RecipientInfo createRecipientInfoFromShipment(Shipments shipment) {
        if (shipment != null) {
            return new OrderDetailResponse.RecipientInfo(
                    shipment.getRecipientName(),
                    shipment.getRecipientPhone(),
                    shipment.getFullShippingAddress(),
                    shipment.getDeliveryNote() != null ? shipment.getDeliveryNote() : "배송 요청사항 없음"
            );
        }
        return new OrderDetailResponse.RecipientInfo("수령인 미등록", "연락처 미등록", "주소 미등록", "배송 요청사항 없음");
    }
}
