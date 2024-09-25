package com.streamcart.cartservice.security;

import java.util.Objects;

public record AuthorizedUser(String userId) {
    static final ThreadLocal<AuthorizedUser> AUTHORIZED_USER_THREAD_LOCAL = new ThreadLocal<>();

    public static AuthorizedUser current() {
        return Objects.requireNonNull(AUTHORIZED_USER_THREAD_LOCAL.get());
    }
}
