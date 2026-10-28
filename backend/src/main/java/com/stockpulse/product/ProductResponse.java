package com.stockpulse.product;

import java.math.BigDecimal;

public record ProductResponse(
        String id,
        String sku,
        String name,
        Category category,
        BigDecimal currentPrice,
        int stockLevel,
        int reorderThreshold,
        int demandVelocity,
        ProductStatus status,
        Long version
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getCurrentPrice(),
                product.getStockLevel(),
                product.getReorderThreshold(),
                product.getDemandVelocity(),
                product.getStatus(),
                product.getVersion()
        );
    }
}