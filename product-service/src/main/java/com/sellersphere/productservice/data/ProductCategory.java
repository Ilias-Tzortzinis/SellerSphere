package com.sellersphere.productservice.data;

import java.util.Optional;

public enum ProductCategory {
    LAPTOP;

    public static Optional<ProductCategory> fromString(String str) {
        return Optional.ofNullable(switch (str) {
            case "laptop" -> LAPTOP;
            default -> null;
        });
    }
}
