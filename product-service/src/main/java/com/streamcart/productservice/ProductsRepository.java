package com.streamcart.productservice;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.streamcart.productservice.model.*;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;

@Component
public final class ProductsRepository {

    private final MongoClient mongoClient;
    private final MongoCollection<Document> products;
    private final ObservationRegistry observationRegistry;

    public ProductsRepository(MongoClient mongoClient, ObservationRegistry observationRegistry) {
        this.mongoClient = mongoClient;
        this.products = mongoClient.getDatabase("streamcart").getCollection("products");
        this.observationRegistry = observationRegistry;
    }

    public List<ProductView> queryProductViews(Product.Category category, ProductQuery query) throws InvalidProductQueryException {
        return Observation.start("queryProductViews", observationRegistry).observeChecked(() -> {
            return products.find(encodeQuery(category, query))
                    .limit(15)
                    .map(doc -> {
                        var productId = doc.getObjectId("_id").toHexString();
                        var name = doc.getString("brand");
                        var quantity = doc.getInteger("quantity");
                        var price = doc.getDouble("price");
                        return new ProductView(productId, name, quantity, price);
                    })
                    .into(new ArrayList<>(15));
        });
    }

    public Optional<Product> findProductById(String productId) throws InvalidProductIdException {
        ObjectId _id;
        try {
            _id = new ObjectId(productId);
        } catch (IllegalArgumentException exc){
            throw new InvalidProductIdException();
        }
        var document = products.find(eq("_id", _id)).first();
        if (document == null) return Optional.empty();
        var category = Product.Category.valueOf(document.getString("category"));
        return Optional.of(decodeDocument(category, document));
    }

    private Product decodeDocument(Product.Category category, Document document){
        var productId = document.getObjectId("_id").toHexString();
        var brand = document.getString("brand");
        var quantity = document.getInteger("quantity");
        var price = document.getDouble("price");
        return switch (category){
            case LAPTOP -> {
                var ram = document.getInteger("ram");
                yield new Laptop(productId, brand, quantity, price, ram);
            }
            case RAM -> {
                var capacity = document.getInteger("capacity");
                var ddr = Ram.DDR.of(document.getString("ddr")).orElseThrow();
                yield new Ram(productId, brand, quantity, price, capacity, ddr);
            }
        };
    }

    private Bson encodeQuery(Product.Category category, ProductQuery query) throws InvalidProductQueryException {
        var filters = new ArrayList<Bson>(6);
        filters.add(eq("category", category.name()));
        if (query.lastId() != null){
            try {
                filters.add(gt("_id", new ObjectId(query.lastId())));
            } catch (IllegalArgumentException exc){
                throw new InvalidProductQueryException("Invalid lastId", exc);
            }
        }
        if (query.brand() != null) filters.add(eq("brand", query.brand()));
        if (query.min_price() != null) filters.add(gte("price", query.min_price()));
        if (query.max_price() != null) filters.add(lte("price", query.max_price()));
        switch (query){
            case ProductQuery.ForLaptop laptop -> {
                if (laptop.ram() != null) filters.add(eq("ram", laptop.ram()));
            }
            case ProductQuery.ForRAM ram -> {
                if (ram.capacity() != null) filters.add(eq("capacity", ram.capacity()));
                if (ram.ddr() != null) filters.add(eq("ddr", ram.ddr().name()));
            }
        }
        return filters.size() == 1 ? filters.getFirst() : and(filters);
    }
}
