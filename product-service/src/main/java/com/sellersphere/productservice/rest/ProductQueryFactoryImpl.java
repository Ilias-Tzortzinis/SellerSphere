package com.sellersphere.productservice.rest;

import com.sellersphere.productservice.data.ProductCategory;
import com.sellersphere.productservice.data.ProductQuery;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public final class ProductQueryFactoryImpl implements ProductQueryFactory {

    @Override
    public ProductQuery create(ProductCategory category, Map<String, String> queryParams) throws InvalidProductQueryException {
        var lastId = queryParams.get("lastId");
        Integer minPrice = getInteger("minPrice", queryParams, true);
        Integer maxPrice = getInteger("maxPrice", queryParams, true);
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new InvalidProductQueryException("minPrice is greater than maxPrice");
        }
        return switch (category){
            case LAPTOP -> {
                var ram = getInteger("ram", queryParams, true);
                yield new ProductQuery.ForLaptop(minPrice, maxPrice, lastId, ram);
            }
        };
    }

    private static Integer getInteger(String key, Map<String, String> query, boolean unsigned) throws InvalidProductQueryException {
        var value = query.get(key);
        if (value == null) return null;
        try {
            return unsigned ? Integer.parseUnsignedInt(value) : Integer.parseInt(value);
        }
        catch (NumberFormatException e){
            throw new InvalidProductQueryException("Invalid integer value for ".concat(key));
        }
    }

}
