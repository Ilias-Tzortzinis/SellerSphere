package com.sellersphere.orderservice.data;

import java.util.List;

public record OrderDetails(String orderId, OrderStatus status, long placedAt, List<OrderItem> items) {

}
