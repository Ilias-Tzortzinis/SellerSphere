package com.sellersphere.cartservice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.Length;

public record CartItem(@NotNull @Length(min = 24, max = 24) String productId,
                       @PositiveOrZero int quantity) {
}
