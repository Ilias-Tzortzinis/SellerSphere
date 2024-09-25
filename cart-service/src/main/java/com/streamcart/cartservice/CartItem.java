package com.streamcart.cartservice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CartItem(@NotNull @NotBlank String productId, @NotNull @NotBlank String name,
                       @PositiveOrZero int quantity) {
}
