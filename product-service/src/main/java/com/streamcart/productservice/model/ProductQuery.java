package com.streamcart.productservice.model;

import java.util.List;

public sealed interface ProductQuery {
    Product.Category category();

    String lastId();

    String brand();

    Integer min_price();

    Integer max_price();

    record ForLaptop(String lastId, String brand, Integer min_price, Integer max_price,
                     Integer ram) implements ProductQuery {
        @Override
        public Product.Category category() {
            return Product.Category.LAPTOP;
        }
    }

    record ForRAM(String lastId, String brand, Integer min_price, Integer max_price,
                  Integer capacity, Ram.DDR ddr) implements ProductQuery {
        @Override
        public Product.Category category() {
            return Product.Category.RAM;
        }
    }

}
