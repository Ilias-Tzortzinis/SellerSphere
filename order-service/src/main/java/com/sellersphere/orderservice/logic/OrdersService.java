package com.sellersphere.orderservice.logic;

import com.sellersphere.orderservice.data.OrderDetials;
import com.sellersphere.orderservice.data.OrderQuery;
import com.sellersphere.orderservice.data.UserOrderView;

import java.util.List;
import java.util.Optional;

public interface OrdersService {

    Optional<OrderDetials> findOrderById(String userId, String orderId);

    List<UserOrderView> findUserOrders(String userId, OrderQuery query);

    OrderDetials placeOrder(String userId) throws OrderPlacementException, EmptyShoppingCartException;

}
