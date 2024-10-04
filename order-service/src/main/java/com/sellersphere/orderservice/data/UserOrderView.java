package com.sellersphere.orderservice.data;

public record UserOrderView(String orderId, OrderStatus status, long placedAt, int totalPrice){}
