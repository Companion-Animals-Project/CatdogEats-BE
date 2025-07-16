package com.team5.catdogeats.orders.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.dto.response.OrderDeleteResponse;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.orders.service.OrderDeleteService;
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
public class OrderDeleteServiceImpl implements OrderDeleteService {
    private final OrderRepository orderRepository;
    private final BuyerRepository buyerRepository;

    @Override
    @JpaTransactional
    public OrderDeleteResponse deleteOrder(UserPrincipal userPrincipal, String orderNumber) {
        log.info("주문 내역 삭제 요청 - provider: {}, providerId: {}, orderNumber: {}",
                userPrincipal.provider(), userPrincipal.providerId(), orderNumber);

        try {
            Buyers buyer = findUserByPrincipal(userPrincipal);
            Orders order = orderRepository.findOrderDetailByUserAndOrderNumber(buyer, orderNumber)
                    .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없거나 접근 권한이 없습니다"));

            if (isDeletionRestricted(order.getOrderStatus())) {
                throw new IllegalArgumentException(getDeletionRestrictionMessage(order.getOrderStatus()));
            }

            if (order.isOrderHidden()) {
                throw new IllegalArgumentException("이미 삭제된 주문 내역입니다");
            }

            order.hideOrder();
            Orders savedOrder = orderRepository.save(order);

            return OrderDeleteResponse.success(
                    savedOrder.getOrderNumber(),
                    savedOrder.getId(),
                    savedOrder.getHiddenAt()
            );

        } catch (NoSuchElementException | IllegalArgumentException e) {
            log.warn("주문 내역 삭제 실패 - orderNumber: {}, reason: {}", orderNumber, e.getMessage());
            return OrderDeleteResponse.failure(orderNumber, e.getMessage());

        } catch (Exception e) {
            log.error("주문 내역 삭제 중 예상치 못한 오류 발생 - orderNumber: {}", orderNumber, e);
            return OrderDeleteResponse.failure(orderNumber, "주문 내역 삭제 중 서버 오류가 발생했습니다");
        }
    }

    private boolean isDeletionRestricted(OrderStatus orderStatus) {
        return List.of(
                OrderStatus.PAYMENT_COMPLETED,
                OrderStatus.PREPARING,
                OrderStatus.READY_FOR_SHIPMENT,
                OrderStatus.IN_DELIVERY,
                OrderStatus.REFUND_PROCESSING
        ).contains(orderStatus);
    }

    private String getDeletionRestrictionMessage(OrderStatus orderStatus) {
        return switch (orderStatus) {
            case PAYMENT_COMPLETED -> "결제가 완료된 주문은 삭제할 수 없습니다. 상품 준비 진행상황을 확인해주세요.";
            case PREPARING -> "상품 준비 중인 주문은 삭제할 수 없습니다.";
            case READY_FOR_SHIPMENT -> "배송 준비가 완료된 주문은 삭제할 수 없습니다.";
            case IN_DELIVERY -> "배송 중인 주문은 삭제할 수 없습니다. 배송 조회 페이지에서 배송상황을 확인해주세요.";
            case REFUND_PROCESSING -> "환불 처리 중인 주문은 삭제할 수 없습니다. 환불 진행상황을 확인해주세요.";
            default -> "현재 상태에서는 주문을 삭제할 수 없습니다.";
        };
    }

    private Buyers findUserByPrincipal(UserPrincipal userPrincipal) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                        userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("구매자를 찾을 수 없거나 권한이 없습니다"));
        return BuyerDTO.toEntity(buyerDTO);
    }

}
