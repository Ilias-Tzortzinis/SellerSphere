package com.sellersphere.orderservice.repository;

import com.sellersphere.orderservice.data.CartItem;

import java.util.List;

public interface UserShoppingCartLoader {
    List<CartItem> loadUserCart(String userId);
}
