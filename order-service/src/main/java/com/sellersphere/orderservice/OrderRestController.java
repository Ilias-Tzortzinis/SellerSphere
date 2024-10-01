package com.sellersphere.orderservice;

import com.sellersphere.authorization.AuthorizedUser;
import com.sellersphere.orderservice.data.OrderDetials;
import com.sellersphere.orderservice.logic.OrderPlacementException;
import com.sellersphere.orderservice.data.OrderQuery;
import com.sellersphere.orderservice.data.UserOrderView;
import com.sellersphere.orderservice.logic.EmptyShoppingCartException;
import com.sellersphere.orderservice.logic.OrdersService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/orders")
public final class OrderRestController {

    private final OrdersService ordersService;

    public OrderRestController(OrdersService ordersService) {
        this.ordersService = ordersService;
    }

    @GetMapping
    public List<UserOrderView> findUserOrders(@Valid OrderQuery orderQuery){
        AuthorizedUser auth = AuthorizedUser.current()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return ordersService.findUserOrders(auth.userId(), orderQuery);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderDetials> findOrderById(@PathVariable String orderId){
        return AuthorizedUser.current().map(auth -> {
            return ordersService.findOrderById(auth.userId(), orderId)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        }).orElseGet(() -> new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(){
        return AuthorizedUser.current().map(auth -> {
            try {
                return ResponseEntity.ok(ordersService.placeOrder(auth.userId()));
            } catch (OrderPlacementException e) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(e);
            } catch (EmptyShoppingCartException e) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        }).orElseGet(() -> new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
    }


}
