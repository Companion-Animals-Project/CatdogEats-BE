package com.team5.catdogeats.orders.event;

import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.users.domain.mapping.Buyers;

public record PaymentTimeoutEvent (String orderId,
                                   Buyers buyer,
                                   String orderNumber,
                                   OrderStatus orderStatus,
                                   Long subTotal,
                                   Long totalDeliveryFee,
                                   Long totalDiscountAmount,
                                   Long discountedTotalPrice){

}
