package com.streamcart.userservice;

public final class UserAlreadyExistsException extends Exception {
    public UserAlreadyExistsException(String msg) {
        super(msg);
    }
}
