package com.sellersphere.authorization;

import java.util.Optional;

public record AuthorizedUser(String userId) {

    static final ThreadLocal<AuthorizedUser> AUTHORIZED_USER_THREAD_LOCAL = new ThreadLocal<>();

    public static Optional<AuthorizedUser> current(){
        return Optional.ofNullable(AUTHORIZED_USER_THREAD_LOCAL.get());
    }

}
