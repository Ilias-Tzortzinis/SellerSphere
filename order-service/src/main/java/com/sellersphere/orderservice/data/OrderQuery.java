package com.sellersphere.orderservice.data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderQuery(String lastId,
                         @Min(2020) Integer year,
                         @Min(1) @Max(12) Integer month,
                         @Min(1) @Max(31) Integer day) {
}
