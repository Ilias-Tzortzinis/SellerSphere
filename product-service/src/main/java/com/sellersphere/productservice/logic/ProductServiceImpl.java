package com.sellersphere.productservice.logic;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.sellersphere.productservice.data.*;
import com.sellersphere.productservice.rest.InvalidProductQueryException;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.include;

@Service
public final class ProductServiceImpl implements ProductService {

    private final MongoCollection<Document> products;

    public ProductServiceImpl(MongoClient mongoClient){
        products = mongoClient.getDatabase("sellersphere").getCollection("products");
    }

    @Override
    public List<ProductOverview> searchProducts(ProductQuery query) throws InvalidProductQueryException {
        return products.find(encodeProductQuery(query)).limit(15)
                .projection(include("_id", "price", "quantity"))
                .map(this::decodeProductOverview)
                .into(new ArrayList<>(15));
    }

    @Override
    public Optional<Product> findProductById(String productId) {
        ObjectId objectId;
        try {
            objectId = new ObjectId(productId);
        } catch (IllegalArgumentException e){
            return Optional.empty();
        }
        var first = products.find(eq("_id", objectId)).first();
        if (first == null) return Optional.empty();
        var category = ProductCategory.valueOf(first.getString("category"));
        return Optional.of(switch (category) {
            case LAPTOP -> decodeProduct(category, first);
        });
    }

    private Product decodeProduct(ProductCategory category, Document document) {
        String productId = document.getObjectId("_id").toHexString();
        int quantity = document.getInteger("quantity");
        double price = document.getDouble("price");
        return switch (category){
            case LAPTOP -> {
                int ram = document.getInteger("ram");
                yield new Laptop(productId, quantity, price, ram);
            }
        };
    }

    private ProductOverview decodeProductOverview(Document doc) {
        var productId = doc.getObjectId("_id").toHexString();
        var price = doc.getDouble("price");
        var quantity = doc.getInteger("quantity");
        return new ProductOverview(productId, quantity, price);
    }

    private Bson encodeProductQuery(ProductQuery query) throws InvalidProductQueryException {
        ArrayList<Bson> filters = new ArrayList<>(6);
        filters.add(eq("category", query.category().name()));
        if (query.lastId() != null) {
            ObjectId lastId;
            try {
                lastId = new ObjectId(query.lastId());
            } catch (IllegalArgumentException e){
                throw new InvalidProductQueryException("Invalid lastId");
            }
            filters.add(gt("_id", lastId));
        }
        if (query.minPrice() != null) filters.add(gte("price", query.minPrice()));
        if (query.maxPrice() != null) filters.add(lte("price", query.maxPrice()));

        switch (query){
            case ProductQuery.ForLaptop forLaptop -> {
                if (forLaptop.ram() != null) filters.add(eq("ram", forLaptop.ram()));
            }
        }
        return filters.size() == 1 ? filters.getFirst() : Filters.and(filters);
    }
}
