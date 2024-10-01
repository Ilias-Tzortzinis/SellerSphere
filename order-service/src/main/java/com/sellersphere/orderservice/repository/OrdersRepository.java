package com.sellersphere.orderservice.repository;

import com.sellersphere.orderservice.data.OrderDetials;
import com.sellersphere.orderservice.data.OrderItem;
import com.sellersphere.orderservice.data.OrderQuery;
import com.sellersphere.orderservice.data.UserOrderView;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface OrdersRepository {
    List<UserOrderView> findOrders(String userId, OrderQuery query);

    String createOrderId(ZonedDateTime dateTime);

    OrderDetials saveOrder(String userId, String orderId, ZonedDateTime dateTime, List<OrderItem> orderItems);

    Optional<OrderDetials> findOrderById(String userId, String orderId);
}
