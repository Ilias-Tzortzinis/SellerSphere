package com.sellersphere.orderservice.repository;

import com.sellersphere.orderservice.data.*;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public final class DynamoDBOrdersRepository implements OrdersRepository {

    private final DynamoDbClient dynamoDB;
    private final SecureRandom secureRandom;

    public DynamoDBOrdersRepository(DynamoDbClient dynamoDB) {
        this.dynamoDB = dynamoDB;
        secureRandom = new SecureRandom();
    }

    @Override
    public Optional<OrderDetails> findOrderById(String userId, String orderId) {
        var item = dynamoDB.getItem(GetItemRequest.builder()
                .tableName("Users")
                .projectionExpression("OrderStatus, PlacedAt, Items")
                .key(Map.of("PK", AttributeValue.fromS(userId), "SK", AttributeValue.fromS(orderId)))
                .build()).item();
        if (item.isEmpty()) return Optional.empty();
        var orderStatus = OrderStatus.valueOf(item.get("OrderStatus").s());
        long placedAt = Long.parseUnsignedLong(item.get("PlacedAt").n());
        List<OrderItem> cartItems = decodeOrderItems(item.get("Items").l());
        return Optional.of(new OrderDetails(orderId, orderStatus, placedAt, cartItems));
    }

    @Override
    public List<UserOrderView> findOrders(String userId, OrderQuery query) {
        var orderQuery = encodeQuery(query);
        var items = dynamoDB.query(QueryRequest.builder()
                .tableName("Users")
                .projectionExpression("SK, OrderStatus, PlacedAt, TotalPrice")
                .keyConditionExpression("PK = :userId AND begins_with(SK, :orderQuery)")
                .applyMutation(b -> {
                    if (query.lastId() != null){
                        b.exclusiveStartKey(Map.of("SK", AttributeValue.fromS(query.lastId())));
                    }
                })
                .expressionAttributeValues(Map.of(
                        ":userId", AttributeValue.fromS(userId),
                        ":orderQuery", AttributeValue.fromS(orderQuery)))
                .build()).items();
        if (items.isEmpty()) return List.of();
        var userOrderViews = new ArrayList<UserOrderView>(items.size());
        for (Map<String, AttributeValue> item : items) userOrderViews.add(decodeOrderView(item));
        return userOrderViews;
    }

    @Override
    public String createOrderId(ZonedDateTime dateTime) {
        var digits = secureRandom.nextInt(111_111, 999_999);
        return "ORDER#" + dateTime.getYear() + width2(dateTime.getMonthValue()) + width2(dateTime.getDayOfMonth()) + digits;
    }

    @Override
    public OrderDetails saveOrder(String userId, String orderId, ZonedDateTime dateTime, List<OrderItem> orderItems) {
        var unixEpoch = dateTime.toEpochSecond();
        int totalPrice = orderItems.stream().mapToInt(OrderItem::price).sum();
        dynamoDB.putItem(PutItemRequest.builder()
                .tableName("Users")
                .conditionExpression("attribute_not_exists(SK)")
                .item(Map.of("PK", AttributeValue.fromS(userId), "SK", AttributeValue.fromS(orderId),
                        "OrderStatus", AttributeValue.fromS(OrderStatus.PENDING_PAYMENT.name()),
                        "PlacedAt", AttributeValue.fromN(String.valueOf(unixEpoch)),
                        "TotalPrice", AttributeValue.fromN(String.valueOf(totalPrice)),
                        "Items", AttributeValue.fromL(encodeOrderItems(orderItems))))
                .build());
        return new OrderDetails(orderId, OrderStatus.PENDING_PAYMENT, unixEpoch, orderItems);
    }

    private static ArrayList<AttributeValue> encodeOrderItems(List<OrderItem> orderItems){
        var values = new ArrayList<AttributeValue>(orderItems.size());
        for (OrderItem orderItem : orderItems) {
            values.add(AttributeValue.fromM(Map.of(
                    "ProductId", AttributeValue.fromS(orderItem.productId()),
                    "ProductName", AttributeValue.fromS(orderItem.productName()),
                    "Quantity", AttributeValue.fromN(String.valueOf(orderItem.quantity())),
                    "Price", AttributeValue.fromN(String.valueOf(orderItem.price())))));
        }
        return values;
    }

    private static List<OrderItem> decodeOrderItems(List<AttributeValue> cartItems) {
        var orderItems = new ArrayList<OrderItem>(cartItems.size());
        for (AttributeValue attributeValue : cartItems) {
            var item = attributeValue.m();
            var productId = item.get("ProductId").s();
            var productName = item.get("ProductName").s();
            var quantity = Integer.parseUnsignedInt(item.get("Quantity").n());
            var price = Integer.parseUnsignedInt(item.get("Price").n());
            orderItems.add(new OrderItem(productId, productName, quantity, price));
        }
        return orderItems;
    }

    private static UserOrderView decodeOrderView(Map<String, AttributeValue> item) {
        String orderId = item.get("SK").s();
        var orderStatus = OrderStatus.valueOf(item.get("OrderStatus").s());
        long unixEpoch = Long.parseUnsignedLong(item.get("PlacedAt").n());
        int totalPrice = Integer.parseUnsignedInt(item.get("TotalPrice").n());
        return new UserOrderView(orderId, orderStatus, unixEpoch, totalPrice);
    }

    private static String encodeQuery(OrderQuery query){
        if (query.year() == null) return "ORDER#";
        if (query.month() == null) return "ORDER#" + query.year();
        if (query.day() ==null) return "ORDER#" + query.year() + width2(query.month());
        return "ORDER#" + query.year() + width2(query.month()) + width2(query.day());
    }

    private static String width2(int n){
        if (n < 10){
            return new String(new byte[]{'0', (byte) (n + '0')}, StandardCharsets.US_ASCII);
        }
        return Integer.toString(n);
    }
}
