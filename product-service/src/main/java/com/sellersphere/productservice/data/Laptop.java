package com.sellersphere.productservice.data;

import java.util.List;

public record Laptop(String productId, String productName, int quantity, int price, int ram,
                     List<String> images, String description) implements Product {

}
