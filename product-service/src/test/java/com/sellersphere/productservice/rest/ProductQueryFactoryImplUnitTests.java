package com.sellersphere.productservice.rest;

import com.sellersphere.productservice.data.ProductCategory;
import com.sellersphere.productservice.data.ProductQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runners.Parameterized;

import java.util.Map;

import static com.sellersphere.productservice.data.ProductCategory.LAPTOP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ProductQueryFactoryImplUnitTests {

    final ProductQueryFactoryImpl factory = new ProductQueryFactoryImpl();

    @Test
    @DisplayName("Create a empty query")
    void createAEmptyQuery() {
        var productQuery = assertDoesNotThrow(() -> factory.create(LAPTOP, Map.of()));

        assertThat(productQuery).isEqualTo(new ProductQuery.ForLaptop(null, null, null, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "15.5", "-10", "2348324821848324321"})
    @DisplayName("MinPrice and MaxPrice must be valid ints if specified")
    void minPriceAndMaxPriceMustBeValidIntsIfSpecified(String price) {
        assertThrows(InvalidProductQueryException.class, () -> factory.create(LAPTOP, Map.of("minPrice", price)));

        assertThrows(InvalidProductQueryException.class, () -> factory.create(LAPTOP, Map.of("maxPrice", price)));
    }

    @Test
    @DisplayName("minPrice must be less than maxPrice")
    void minPriceMustBeLessThanMaxPrice() {
        assertThrows(InvalidProductQueryException.class, () -> factory.create(LAPTOP, Map.of("minPrice", "50", "maxPrice", "40")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5", "15.5", "asdfs", "47537513949824891"})
    @DisplayName("Laptops ram must be positive")
    void laptopsRamMustBePositive(String ram) {
        assertThrows(InvalidProductQueryException.class, () -> factory.create(LAPTOP, Map.of("ram", ram)));
    }


}