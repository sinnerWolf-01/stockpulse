package com.stockpulse.product;

import jakarta.validation.constraints.Min;

public record OrderRequest(
        @Min(1) Integer quantity
) {
    public int resolvedQuantity() {
        return quantity == null ? 1 : quantity;
    }
}