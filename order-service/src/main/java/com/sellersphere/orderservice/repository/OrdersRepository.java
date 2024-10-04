package com.sellersphere.orderservice.repository;

import com.sellersphere.orderservice.data.OrderDetails;
import com.sellersphere.orderservice.data.OrderItem;
import com.sellersphere.orderservice.data.OrderQuery;
import com.sellersphere.orderservice.data.UserOrderView;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface OrdersRepository {
    List<UserOrderView> findOrders(String userId, OrderQuery query);

    String createOrderId(ZonedDateTime dateTime);

    OrderDetails saveOrder(String userId, String orderId, ZonedDateTime dateTime, List<OrderItem> orderItems);

    Optional<OrderDetails> findOrderById(String userId, String orderId);
}
