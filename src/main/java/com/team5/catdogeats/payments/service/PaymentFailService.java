package com.team5.catdogeats.payments.service;

public interface PaymentFailService {
    void handlePaymentFailure(String orderId, String code, String message);
}
