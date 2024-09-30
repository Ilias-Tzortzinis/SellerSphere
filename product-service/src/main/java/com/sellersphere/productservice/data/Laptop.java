package com.sellersphere.productservice.data;

public record Laptop(String productId, int quantity, double price, int ram) implements Product {

}
