package com.streamcart.productservice.model;

public final class InvalidProductQueryException extends Exception {
    public InvalidProductQueryException(String message) {
        super(message);
    }

    public InvalidProductQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
