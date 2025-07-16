package com.team5.catdogeats.payments.util;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.payments.domain.Payments;
import com.team5.catdogeats.payments.domain.enums.PaymentStatus;
import com.team5.catdogeats.payments.dto.response.TossPaymentConfirmResponse;

public class PaymentUtils {

    public static void validatePaymentStatus(Payments payment, Orders order) {
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("결제가 이미 처리되었습니다: " + payment.getStatus());
        }

        if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("주문 상태가 결제 대기가 아닙니다: " + order.getOrderStatus());
        }
    }

    public static void validatePaymentAmount(Orders order, Long amount) {
        if (!order.getDiscountedTotalPrice().equals(amount)) {
            throw new IllegalArgumentException(
                    String.format("결제 금액이 일치하지 않습니다. 주문금액: %d, 결제요청금액: %d",
                            order.getTotalDiscountAmount(), amount)
            );
        }
    }

    public static void validateTossResponse(TossPaymentConfirmResponse response, Orders order, Long amount) {
        if (response == null) {
            throw new RuntimeException("Toss Payments API 응답이 null입니다");
        }

        if (!response.getOrderId().equals(order.getId())) {
            throw new RuntimeException("응답의 주문 ID가 일치하지 않습니다");
        }

        if (!response.getTotalAmount().equals(amount)) {
            throw new RuntimeException("응답의 결제 금액이 일치하지 않습니다");
        }
    }

}
