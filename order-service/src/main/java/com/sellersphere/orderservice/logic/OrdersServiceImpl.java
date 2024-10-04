package com.sellersphere.orderservice.logic;

import com.sellersphere.orderservice.data.*;
import com.sellersphere.orderservice.invalidation.OrderInvalidationService;
import com.sellersphere.orderservice.repository.OrdersRepository;
import com.sellersphere.orderservice.repository.UserShoppingCartLoader;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;
import java.util.Optional;

@Service
public final class OrdersServiceImpl implements OrdersService {

    private final OrdersRepository ordersRepository;
    private final UserShoppingCartLoader userCartLoader;
    private final OrderInvalidationService orderInvalidationService;
    private final ProductsManager productsManager;

    public OrdersServiceImpl(OrdersRepository ordersRepository, ProductsManager productsManager, UserShoppingCartLoader userCartLoader, OrderInvalidationService orderInvalidationService) {
        this.ordersRepository = ordersRepository;
        this.productsManager = productsManager;
        this.userCartLoader = userCartLoader;
        this.orderInvalidationService = orderInvalidationService;
    }

    @Override
    public Optional<OrderDetails> findOrderById(String userId, String orderId) {
        return ordersRepository.findOrderById(userId, orderId);
    }

    @Override
    public List<UserOrderView> findUserOrders(String userId, OrderQuery query) {
        return ordersRepository.findOrders(userId, query);
    }

    @Override
    public OrderDetails placeOrder(String userId) throws OrderPlacementException, EmptyShoppingCartException {
        var dateTime = ZonedDateTime.now(Clock.systemUTC());
        var cartItems = userCartLoader.loadUserCart(userId);
        if (cartItems.isEmpty()) throw new EmptyShoppingCartException();
        return productsManager.reserveProductsStock(cartItems, orderItems -> {
            var orderId = ordersRepository.createOrderId(dateTime);
            orderInvalidationService.schedule(userId, orderId, dateTime);
            return ordersRepository.saveOrder(userId, orderId, dateTime, orderItems);
        });
    }

}
