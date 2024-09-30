package com.sellersphere.productservice.data;

public sealed interface ProductQuery {
    Integer minPrice();

    Integer maxPrice();

    String lastId();

    default ProductCategory category() {
        return switch (this) {
            case ForLaptop _ -> ProductCategory.LAPTOP;
        };
    }

    record ForLaptop(Integer minPrice, Integer maxPrice, String lastId,
                     Integer ram) implements ProductQuery {
    }
}
