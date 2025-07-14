package com.team5.catdogeats.orders.dto.common;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.payments.domain.Payments;

public record GroupOrdersAndPayments(Orders orders, Payments payments) {
}
