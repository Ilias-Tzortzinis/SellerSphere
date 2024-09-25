package com.streamcart.cartservice;

import com.streamcart.cartservice.security.AuthorizedUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public final class CartRestController {

    private final CartService cartService;

    public CartRestController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public List<CartItem> getUserCart(){
        var auth = AuthorizedUser.current();
        return cartService.getCart(auth.userId());
    }

    @PatchMapping
    public void updateUserCart(@RequestBody @Valid CartItem cartItem){
        var auth = AuthorizedUser.current();
        if (cartItem.quantity() == 0) cartService.deleteCartItem(auth.userId(), cartItem.productId());
        else cartService.updateCart(auth.userId(), cartItem);
    }

}
