package com.streamcart.productservice.model;

import java.util.Map;
import java.util.Optional;

public record Ram(String productId, String brand, int quantity, double price,
                  int capacity, DDR ddr) implements Product {
    @Override
    public Category category() {
        return Category.RAM;
    }

    public enum DDR {
        DD3, DDR4, DDR5;

        private static final Map<String, DDR> LOOKUP_TABLE = Product.lookupTable(DDR.class, DDR::name);

        public static Optional<DDR> of(String name){
            return Optional.ofNullable(LOOKUP_TABLE.get(name));
        }

    }
}
