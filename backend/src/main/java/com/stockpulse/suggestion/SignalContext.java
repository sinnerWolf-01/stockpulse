package com.stockpulse.suggestion;

import com.stockpulse.product.Category;

import java.math.BigDecimal;

public record SignalContext(
        String productId,
        Category category,
        BigDecimal currentPrice,
        int stockLevel,
        int reorderThreshold,
        int demandVelocity,
        BigDecimal categoryPeerAverage,
        TriggerReason triggerReason) {

    public SignalContext {
        if (productId == null || productId.isBlank()) throw new IllegalArgumentException("productId is required");
        if (category == null || currentPrice == null || categoryPeerAverage == null || triggerReason == null) {
            throw new IllegalArgumentException("Signal context metadata is required");
        }
        if (stockLevel < 0 || reorderThreshold < 0 || demandVelocity < 0) {
            throw new IllegalArgumentException("Signal values cannot be negative");
        }
    }
}