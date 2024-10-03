package com.sellersphere.paymentservice;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Currency;
import java.util.List;

@Service
public final class StripePaymentsService implements PaymentsService {

    private final OrderService orderLoader;

    public StripePaymentsService(OrderService orderService, @Value("${STRIPE_API_KEY}") String stripeKey) {
        this.orderLoader = orderService;
        Stripe.apiKey = stripeKey;
    }

    @Override
    public void createPayment(String userId, String orderId) {
        var orderDetials = orderLoader.findAndUpdateOrderStatus(userId, orderId);

        var params = SessionCreateParams.builder()
                .setSuccessUrl("/payments/success")
                .setUiMode(SessionCreateParams.UiMode.HOSTED)
                .setCurrency("EUR");

        orderDetials.items().forEach(orderItem -> params.addLineItem(SessionCreateParams.LineItem.builder()
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("EUR")
                                .setUnitAmount((long) orderItem.quantity() * orderItem.price())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(orderItem.productName())
                                        .build())
                                .build())
                .build()));
    }


    public interface OrderService {
        OrderDetials findAndUpdateOrderStatus(String userId, String orderId);
    }

    public record OrderDetials(String orderId, OrderStatus status, long unixEpoch, List<OrderItem> items){}

    public enum OrderStatus {
        PENDING_PAYMENT,
        PAYMENT_IN_PROGRESS,

        PAYMENT_COMPLETE,
        PAYMENT_FAILED
    }

    public record OrderItem(String productId, String productName, int quantity, int price){}
}
