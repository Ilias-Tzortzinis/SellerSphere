package com.streamcart.productservice.model;

public record Laptop(String productId, String brand, int quantity, double price,
                     int ram) implements Product {

    @Override
    public Category category() {
        return Category.LAPTOP;
    }
}
