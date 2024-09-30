package com.sellersphere.cartservice;

import java.util.List;

public interface CartService {

    List<CartItem> getUserCart(String userId);

    void updateUserCart(String userId, CartItem cartItem);
}
