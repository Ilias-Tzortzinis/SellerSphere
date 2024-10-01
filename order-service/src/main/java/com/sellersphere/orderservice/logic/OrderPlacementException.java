package com.sellersphere.orderservice.logic;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
        @JsonSubTypes.Type(OrderPlacementException.NotEnoughProductStockException.class),
        @JsonSubTypes.Type(OrderPlacementException.ProductNotFoundException.class)
})
public abstract sealed class OrderPlacementException extends Exception {

    public static final class NotEnoughProductStockException extends OrderPlacementException {
        public final String productId;

        public NotEnoughProductStockException(String productId) {
            this.productId = productId;
        }
    }

    public static final class ProductNotFoundException extends OrderPlacementException {
        public final String productId;

        public ProductNotFoundException(String productId) {
            this.productId = productId;
        }
    }


}
