package com.sellersphere.orderservice.logic;

import com.sellersphere.orderservice.data.OrderDetails;
import com.sellersphere.orderservice.data.OrderQuery;
import com.sellersphere.orderservice.data.UserOrderView;

import java.util.List;
import java.util.Optional;

public interface OrdersService {

    Optional<OrderDetails> findOrderById(String userId, String orderId);

    List<UserOrderView> findUserOrders(String userId, OrderQuery query);

    OrderDetails placeOrder(String userId) throws OrderPlacementException, EmptyShoppingCartException;

}
