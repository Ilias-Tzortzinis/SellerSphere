package com.sellersphere.orderservice.data;

import java.util.List;

public record OrderDetials(String orderId, OrderStatus status, long unixEpoch, List<OrderItem> items) {

}
