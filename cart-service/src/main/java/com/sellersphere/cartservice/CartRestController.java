package com.sellersphere.cartservice;

import com.sellersphere.authorization.AuthorizedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/cart")
public final class CartRestController {

    private final CartService cartService;

    public CartRestController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<List<CartItem>> getUserCart(){
        return AuthorizedUser.current().map(auth -> {
            return ResponseEntity.ok(cartService.getUserCart(auth.userId()));
        }).orElseGet(() -> new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
    }

    @PatchMapping
    public void updateUserCart(@RequestBody @Valid CartItem cartItem){
        var auth = AuthorizedUser.current().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        cartService.updateUserCart(auth.userId(), cartItem);
    }

}
