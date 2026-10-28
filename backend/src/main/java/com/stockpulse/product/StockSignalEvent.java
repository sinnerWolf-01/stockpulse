package com.stockpulse.product;

public record StockSignalEvent(
        String productId,
        int stockLevel,
        int reorderThreshold,
        int demandVelocity,
        StockSignalSource source
) {

    public StockSignalEvent {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("Stock signal productId is required");
        }
        if (stockLevel < 0) {
            throw new IllegalArgumentException("Stock signal stockLevel cannot be negative");
        }
        if (reorderThreshold < 0) {
            throw new IllegalArgumentException("Stock signal reorderThreshold cannot be negative");
        }
        if (demandVelocity < 0) {
            throw new IllegalArgumentException("Stock signal demandVelocity cannot be negative");
        }
        if (source == null) {
            throw new IllegalArgumentException("Stock signal source is required");
        }
    }

    /**
     * Whether this mutation is allowed to enter asynchronous suggestion evaluation.
     */
    public boolean triggersEvaluation() {
        return source.triggersEvaluation();
    }
}