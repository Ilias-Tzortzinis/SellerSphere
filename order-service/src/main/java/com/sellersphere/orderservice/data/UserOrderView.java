package com.sellersphere.orderservice.data;

import java.util.List;

public record UserOrderView(String orderId, long unixEpoch, double totalPrice){}
