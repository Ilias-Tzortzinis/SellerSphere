package com.sellersphere.userservice.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UserSignupData(@NotNull @Valid UserCredentials credentials) {
}
