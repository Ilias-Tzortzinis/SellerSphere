package com.sellersphere.cartservice;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public final class CartServiceImpl implements CartService {

    public static final AttributeValue CART_ITEM_PREFIX = AttributeValue.fromS("CART_ITEM#");
    private final DynamoDbClient dynamoDB;

    public CartServiceImpl(DynamoDbClient dynamoDB) {
        this.dynamoDB = dynamoDB;
    }

    @Override
    public List<CartItem> getUserCart(String userId) {
        var query = dynamoDB.query(QueryRequest.builder()
                .tableName("Users")
                .projectionExpression("SK, Quantity")
                .keyConditionExpression("PK = :userId AND begins_with(SK, :cartItemPrefix)")
                .expressionAttributeValues(Map.of(":userId", AttributeValue.fromS(userId), ":cartItemPrefix", CART_ITEM_PREFIX))
                .build()).items();
        if (query.isEmpty()) return List.of();
        var cart = new ArrayList<CartItem>(query.size());
        for (Map<String, AttributeValue> item : query) {
            String productId = item.get("SK").s().substring(CART_ITEM_PREFIX.s().length());
            int quantity = Integer.parseUnsignedInt(item.get("Quantity").n());
            cart.add(new CartItem(productId, quantity));
        }
        return cart;
    }

    @Override
    public void updateUserCart(String userId, CartItem cartItem) {
        var sk = AttributeValue.fromS(CART_ITEM_PREFIX.s().concat(cartItem.productId()));
        if (cartItem.quantity() == 0){
            // delete the item
            dynamoDB.deleteItem(DeleteItemRequest.builder()
                    .tableName("Users")
                    .key(Map.of("PK", AttributeValue.fromS(userId), "SK", sk))
                    .build());
            return;
        }
        dynamoDB.updateItem(UpdateItemRequest.builder()
                .tableName("Users")
                .key(Map.of("PK", AttributeValue.fromS(userId), "SK", sk))
                .updateExpression("SET Quantity = :quantity")
                .expressionAttributeValues(Map.of(":quantity", AttributeValue.fromN(String.valueOf(cartItem.quantity()))))
                .build());
    }
}
