package com.sellersphere.productservice.rest;

public final class InvalidProductQueryException extends Exception {
    public InvalidProductQueryException(String msg) {
        super(msg);
    }
}
