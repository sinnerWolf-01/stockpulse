package com.stockpulse.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        @NotNull Category category,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal currentPrice,
        @NotNull @Min(0) Integer stockLevel,
        @NotNull @Min(0) Integer reorderThreshold
) {
}