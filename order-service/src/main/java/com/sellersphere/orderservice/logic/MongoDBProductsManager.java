package com.sellersphere.orderservice.logic;

import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.sellersphere.orderservice.data.CartItem;
import com.sellersphere.orderservice.data.OrderItem;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static java.util.function.Predicate.not;

@Component
public final class MongoDBProductsManager implements ProductsManager {

    private final MongoClient mongoClient;
    private final MongoCollection<Document> products;

    public MongoDBProductsManager(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
        this.products = mongoClient.getDatabase("sellersphere").getCollection("products");
    }

    @Override
    public <T> T reserveProductsStock(List<CartItem> cartItems, Function<List<OrderItem>, T> action) throws OrderPlacementException {
        var cartMap = groupCartIntoObjectIdToQuantity(cartItems);
        try (ClientSession transaction = mongoClient.startSession()) {
            transaction.startTransaction();

            var result = action.apply(reserveStock(cartMap, transaction));

            transaction.commitTransaction();

            return result;
        }
    }

    private HashMap<ObjectId, Integer> groupCartIntoObjectIdToQuantity(List<CartItem> cartItems) throws OrderPlacementException.ProductNotFoundException {
        var map = HashMap.<ObjectId, Integer>newHashMap(cartItems.size());
        for (CartItem cartItem : cartItems) {
            ObjectId objectId;
            try {
                objectId = new ObjectId(cartItem.productId());
            } catch (IllegalArgumentException e){
                throw new OrderPlacementException.ProductNotFoundException(cartItem.productId());
            }
            map.put(objectId, cartItem.quantity());
        }
        return map;
    }

    private ArrayList<OrderItem> reserveStock(HashMap<ObjectId, Integer> requiredItems, ClientSession session) throws OrderPlacementException.ProductNotFoundException, OrderPlacementException.NotEnoughProductStockException {
        var stockItems = findRequredItems(requiredItems);
        var orderItems = new ArrayList<OrderItem>(stockItems.size());
        for (Document stockItem : stockItems) {
            var productId = stockItem.getObjectId("_id");
            int requiredStock = requiredItems.get(productId);
            if (reserveStockForProduct(productId, requiredStock, stockItem, session)){
                String name = stockItem.getString("productName");
                int price = stockItem.getInteger("price");
                orderItems.add(new OrderItem(productId.toHexString(), name, requiredStock, price));
                continue;
            }
            orderItems.add(reserveStockForProductWithRetries(productId, requiredStock, session));
        }
        return orderItems;
    }

    private ArrayList<Document> findRequredItems(HashMap<ObjectId, Integer> requiredItems) throws OrderPlacementException.ProductNotFoundException {
        var stockItems = products.find(Filters.in("_id", requiredItems.keySet()))
                .projection(include("_id", "productName", "quantity", "price", "version"))
                .into(new ArrayList<>(requiredItems.size()));
        if (stockItems.size() != requiredItems.size()){
            var missingObjectId = stockItems.stream()
                    .map(doc -> doc.getObjectId("_id"))
                    .filter(not(requiredItems::containsKey))
                    .findFirst().orElseThrow();
            throw new OrderPlacementException.ProductNotFoundException(missingObjectId.toHexString());
        }
        return stockItems;
    }

    private OrderItem reserveStockForProductWithRetries(ObjectId productId, int requiredStock, ClientSession session) throws OrderPlacementException.ProductNotFoundException, OrderPlacementException.NotEnoughProductStockException {
        int retries = 5;
        do {
            var document = products.find(session, eq("_id", productId))
                    .projection(include("productName", "quantity", "price", "version"))
                    .first();
            if (document == null) throw new OrderPlacementException.ProductNotFoundException(productId.toHexString());
            if (reserveStockForProduct(productId, requiredStock, document, session)){
                String productName = document.getString("productName");
                int price = document.getInteger("price");
                return new OrderItem(productId.toHexString(), productName, requiredStock, price);
            }
            retries--;
        } while (retries > 0);
        throw new IllegalStateException("Exceeded max retries(5) to reserve quantity(%d) for product with id(%s)"
                .formatted(requiredStock, productId));
    }

    private boolean reserveStockForProduct(ObjectId productId, int requiredStock, Document stockItem, ClientSession session) throws OrderPlacementException.NotEnoughProductStockException {
        int stock = stockItem.getInteger("quantity");
        if (stock < requiredStock) throw new OrderPlacementException.NotEnoughProductStockException(productId.toHexString());
        int version = stockItem.getInteger("version");
        var result = products.updateOne(session,
                Filters.and(eq("_id", productId), eq("version", version)),
                Updates.combine(set("quantity", stock - requiredStock), inc("version", 1)));
        return result.getModifiedCount() > 0;
    }
}
