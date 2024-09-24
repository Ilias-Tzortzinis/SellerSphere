package com.streamcart.userservice;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCredentials(@NotNull @Email String email,
                              @NotNull @NotBlank @Size(min = 6, max = 16) String password) {
}
