package com.streamcart.userservice;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountVerificationPayload(@NotNull @Email String email,
                                         @NotNull @NotBlank String verificationCode) {
}
