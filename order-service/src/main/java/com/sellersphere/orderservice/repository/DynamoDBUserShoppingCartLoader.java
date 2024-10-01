package com.sellersphere.orderservice.repository;

import com.sellersphere.orderservice.data.CartItem;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public final class DynamoDBUserShoppingCartLoader implements UserShoppingCartLoader {

    public static final AttributeValue CART_ITEM_PREFIX = AttributeValue.fromS("CART_ITEM#");
    private final DynamoDbClient dynamoDB;

    public DynamoDBUserShoppingCartLoader(DynamoDbClient dynamoDB) {
        this.dynamoDB = dynamoDB;
    }

    @Override
    public List<CartItem> loadUserCart(String userId) {
        var items = dynamoDB.query(QueryRequest.builder()
                .tableName("Users")
                .keyConditionExpression("PK = :userId AND begins_with(SK, :cartItemPrefix)")
                .projectionExpression("SK, Quantity")
                .expressionAttributeValues(Map.of(
                        ":userId", AttributeValue.fromS(userId), ":cartItemPrefix", CART_ITEM_PREFIX))
                .build()).items();
        if (items.isEmpty()) return List.of();
        var cartItems = new ArrayList<CartItem>(items.size());
        for (Map<String, AttributeValue> item : items) {
            cartItems.add(decodeItem(item));
        }
        return cartItems;
    }

    private CartItem decodeItem(Map<String, AttributeValue> item) {
        String productId = item.get("SK").s().substring(CART_ITEM_PREFIX.s().length());
        int quantity = Integer.parseUnsignedInt(item.get("Quantity").n());
        return new CartItem(productId, quantity);
    }
}
