package com.streamcart.cartservice;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public final class CartService {

    public static final AttributeValue CART_ITEM_PREFIX = AttributeValue.fromS("CART_ITEM#");


    private final DynamoDbClient dynamoDB;
    private final ObservationRegistry observationRegistry;

    public CartService(DynamoDbClient dynamoDB, ObservationRegistry observationRegistry) {
        this.dynamoDB = dynamoDB;
        this.observationRegistry = observationRegistry;
    }

    public List<CartItem> getCart(String userId) {
        return Observation.start("getCart", observationRegistry).highCardinalityKeyValue("userId", userId).observe(() -> {
            var cartItemValues = dynamoDB.query(QueryRequest.builder()
                    .tableName("USERS")
                    .keyConditionExpression("PK = :userId AND begins_with(SK, :cartItemPrefix)")
                    .projectionExpression("SK, PRODUCT_NAME, QUANTITY")
                    .expressionAttributeValues(Map.of(":userId", AttributeValue.fromS(userId),
                            ":cartItemPrefix", CART_ITEM_PREFIX))
                    .build()).items();
            if (cartItemValues.isEmpty()) return List.of();
            var cart = new ArrayList<CartItem>(cartItemValues.size());
            for (Map<String, AttributeValue> values : cartItemValues) {
                var productId = values.get("SK").s().substring(CART_ITEM_PREFIX.s().length());
                var name = values.get("PRODUCT_NAME").s();
                var quantity = Integer.parseUnsignedInt(values.get("QUANTITY").n());
                cart.add(new CartItem(productId, name, quantity));
            }
            return cart;
        });
    }

    public void updateCart(String userId, CartItem item) {
        Observation.start("updateCart", observationRegistry).highCardinalityKeyValue("userId", userId).observe(() -> {
            dynamoDB.updateItem(UpdateItemRequest.builder()
                    .tableName("USERS")
                    .key(Map.of("PK", AttributeValue.fromS(userId),
                            "SK", AttributeValue.fromS(CART_ITEM_PREFIX.s().concat(item.productId()))))
                    .updateExpression("SET PRODUCT_NAME = :productName, QUANTITY = :quantity")
                    .expressionAttributeValues(Map.of(":productName", AttributeValue.fromS(item.name()),
                            ":quantity", AttributeValue.fromN(String.valueOf(item.quantity()))))
                    .build());
        });
    }

    public void deleteCartItem(String userId, String productId) {
        Observation.start("deleteCartItem", observationRegistry)
                .highCardinalityKeyValue("userId", userId)
                .lowCardinalityKeyValue("productId", productId)
                .observe(() -> {
                    dynamoDB.deleteItem(DeleteItemRequest.builder()
                            .tableName("USERS")
                            .key(Map.of("PK", AttributeValue.fromS(userId),
                                    "SK", AttributeValue.fromS(CART_ITEM_PREFIX.s().concat(productId))))
                            .build());
                });

    }
}
