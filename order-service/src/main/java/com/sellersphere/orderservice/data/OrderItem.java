package com.sellersphere.orderservice.data;

public record OrderItem(String productId, String productName, int quantity, double price) {
}
