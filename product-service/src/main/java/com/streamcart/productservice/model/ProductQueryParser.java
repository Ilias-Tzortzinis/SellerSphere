package com.streamcart.productservice.model;

import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

import java.util.List;

@Component
public final class ProductQueryParser {

    public ProductQuery parse(Product.Category category,
                              MultiValueMap<String, String> query) throws InvalidProductQueryException {
        String lastId = onlyOne(query, "lastId");
        String brand = onlyOne(query, "brand");
        Integer min_price = parseUnsignedInt("min_price", onlyOne(query, "min_price"));
        Integer max_price = parseUnsignedInt("max_price", onlyOne(query, "max_price"));
        if (max_price != null && min_price != null && min_price > max_price ){
            throw new InvalidProductQueryException("min_price > max_price");
        }
        return switch (category){
            case LAPTOP -> {
                var ram = parseUnsignedInt("ram", onlyOne(query, "ram"));
                yield new ProductQuery.ForLaptop(lastId, brand, min_price, max_price, ram);
            }
            case RAM -> {
                var capacity = parseUnsignedInt("capacity", onlyOne(query, "capacity"));
                var ddr = Ram.DDR.of(onlyOne(query, "ddr")).orElseThrow(() -> new InvalidProductQueryException("Invalid ddr"));
                yield new ProductQuery.ForRAM(lastId, brand, min_price, max_price, capacity, ddr);
            }
        };
    }

    private static String onlyOne(MultiValueMap<String, String> query, String key) throws InvalidProductQueryException {
        var list = query.getOrDefault(key, List.of());
        if (list.isEmpty()) return null;
        if (list.size() == 1) return list.getFirst();
        throw new InvalidProductQueryException("Expected only one query value for: ".concat(key));
    }

    private static Integer parseUnsignedInt(String key, String value) throws InvalidProductQueryException {
        if (value == null) return null;
        try {
            return Integer.parseUnsignedInt(value);
        }catch (NumberFormatException e){
            throw new InvalidProductQueryException("Invalid value for: ".concat(key), e);
        }
    }
}
