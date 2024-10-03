package com.sellersphere.paymentservice;

public interface PaymentsService {
    void createPayment(String userId, String orderId);
}
