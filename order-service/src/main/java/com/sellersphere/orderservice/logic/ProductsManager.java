package com.sellersphere.orderservice.logic;

import com.sellersphere.orderservice.data.CartItem;
import com.sellersphere.orderservice.data.OrderItem;

import java.util.List;
import java.util.function.Function;

public interface ProductsManager {
    <T> T reserveProductsStock(List<CartItem> cartItems, Function<List<OrderItem>, T> action) throws OrderPlacementException;
}
